package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.generation.GenerationQuota
import com.emm.domain.validation.DomainValidationException
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val generationDependencies: NewCardGenerationDependencies,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
    private val generationQuota: GenerationQuota,
) : MviViewModel<NewCardUiState, NewCardUiIntent, NewCardUiEffect>(
    initialState = NewCardUiState(),
) {

    private val draftEditor = NewCardDraftEditor(generationDependencies.validateGeneratedLearningNoteUseCase)
    private val previewWorkflow = NewCardPreviewWorkflow(generationDependencies)

    init {
        getDecksUseCase()
            .onEach { decks ->
                val defaultDeckId = defaultDeckSelectionRepository.getDefaultDeckId()
                val selectedDeck = decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
                setState {
                    copy(
                        decks = decks,
                        deckSelected = selectedDeck,
                        isSetAsDefault = defaultDeckId != null && selectedDeck?.id == defaultDeckId,
                    )
                }
            }
            .launchIn(viewModelScope)

        setState { copy(quotaRemaining = generationQuota.remainingToday()) }
    }

    override fun onIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.DeckSelected,
            is NewCardUiIntent.CheckChanged -> handleDeckIntent(intent)
            is NewCardUiIntent.WordChanged,
            is NewCardUiIntent.IntendedMeaningChanged,
            is NewCardUiIntent.ContextSentenceChanged,
            is NewCardUiIntent.DifficultySelected -> handleInputIntent(intent)
            NewCardUiIntent.GenerateClicked -> generateFlashcard()
            NewCardUiIntent.SaveClicked -> saveFlashcard()
            is NewCardUiIntent.PreviewFieldChanged,
            is NewCardUiIntent.PreviewCardPromptChanged,
            is NewCardUiIntent.PreviewCardExpectedAnswerChanged,
            is NewCardUiIntent.PreviewCardHintChanged,
            is NewCardUiIntent.PreviewCardActiveChanged -> handlePreviewEditIntent(intent)
            NewCardUiIntent.RegenerateExampleClicked,
            NewCardUiIntent.RegenerateClozeClicked,
            is NewCardUiIntent.RegenerateFieldClicked,
            is NewCardUiIntent.RegenerateCardClicked -> handleRegenerationIntent(intent)
        }
    }

    private fun handleDeckIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.DeckSelected -> setState {
                copy(
                    deckSelected = intent.deck,
                    isSetAsDefault = defaultDeckSelectionRepository.getDefaultDeckId() == intent.deck.id,
                )
            }
            is NewCardUiIntent.CheckChanged -> {
                val newDeckId = if (intent.checked) currentState.deckSelected?.id else null
                defaultDeckSelectionRepository.setDefaultDeckId(newDeckId)
                setState { copy(isSetAsDefault = intent.checked) }
            }
            else -> Unit
        }
    }

    private fun handleInputIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.WordChanged -> updateInputState { copy(word = intent.word) }
            is NewCardUiIntent.IntendedMeaningChanged -> {
                updateInputState { copy(intendedMeaningEs = intent.intendedMeaningEs) }
            }
            is NewCardUiIntent.ContextSentenceChanged -> {
                updateInputState { copy(contextSentence = intent.contextSentence) }
            }
            is NewCardUiIntent.DifficultySelected -> updateInputState { copy(difficulty = intent.difficulty) }
            else -> Unit
        }
    }

    private fun handlePreviewEditIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.PreviewFieldChanged -> setState {
                draftEditor.editField(this, intent.field, intent.value)
            }
            is NewCardUiIntent.PreviewCardPromptChanged -> setState {
                draftEditor.editCardPrompt(this, intent.cardId, intent.prompt)
            }
            is NewCardUiIntent.PreviewCardExpectedAnswerChanged -> setState {
                draftEditor.editCardExpectedAnswer(this, intent.cardId, intent.expectedAnswer)
            }
            is NewCardUiIntent.PreviewCardHintChanged -> setState {
                draftEditor.editCardHint(this, intent.cardId, intent.hint)
            }
            is NewCardUiIntent.PreviewCardActiveChanged -> setState {
                draftEditor.editCardActive(this, intent.cardId, intent.isActive)
            }
            else -> Unit
        }
    }

    private fun handleRegenerationIntent(intent: NewCardUiIntent) {
        when (intent) {
            NewCardUiIntent.RegenerateExampleClicked -> regenerateExample()
            NewCardUiIntent.RegenerateClozeClicked -> regenerateCloze()
            is NewCardUiIntent.RegenerateFieldClicked -> regenerateField(intent.field)
            is NewCardUiIntent.RegenerateCardClicked -> regenerateCard(intent.cardId)
            else -> Unit
        }
    }

    private fun updateInputState(transform: NewCardUiState.() -> NewCardUiState) {
        setState { transform().clearPreviewState(error = null) }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        if (currentState.isLoading) return@launch
        setState { clearPreviewState(error = null, isLoading = true) }
        sendEffect(NewCardUiEffect.OpenReview)

        when (val result = previewWorkflow.generate(currentState)) {
            is NewCardPreviewResult.InputInvalid -> {
                setState {
                    clearPreviewState(error = null).copy(
                        inputValidationIssues = result.errors,
                        inputWarningIssues = result.warnings,
                    )
                }
            }
            is NewCardPreviewResult.PreviewReady -> {
                setState {
                    withPreviewValidation(result.preview, result.validation)
                        .copy(quotaRemaining = generationQuota.remainingToday())
                }
            }
            is NewCardPreviewResult.DomainError -> {
                setState {
                    clearPreviewState(
                        error = NewCardErrorUi(
                            title = "Couldn't process the AI response",
                            validationIssues = result.issues,
                        ),
                    )
                }
            }
            is NewCardPreviewResult.UnexpectedError -> {
                val error = result.error
                logError(TAG, "generateFlashcard:error ${error.message}", error)
                val classified = NewCardErrorClassifier.classifyGenerationFailure(
                    error = error,
                    fallbackMessage = "Couldn't generate the card. Try again.",
                )
                setState {
                    copy(
                        error = NewCardErrorUi(
                            title = classified.title,
                            message = classified.message,
                            quotaResetAt = classified.quotaResetAt,
                        ),
                        isLoading = false,
                        canSavePreview = false,
                        previewGeneratedWarnings = emptyList(),
                        previewValidationIssues = emptyList(),
                        previewWarningIssues = emptyList(),
                        previewRegenerationTarget = null,
                    )
                }
            }
        }
    }

    private fun regenerateExample() = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Example,
            actionName = "regenerateExample",
            failureTitle = "Couldn't regenerate the example",
            fallbackMessage = "Couldn't regenerate the example. Try again.",
        ) { previewWorkflow.regenerateExample(currentState) }
    }

    private fun regenerateCloze() = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Cloze,
            actionName = "regenerateCloze",
            failureTitle = "Couldn't regenerate",
            fallbackMessage = "Couldn't regenerate. Try again.",
        ) { previewWorkflow.regenerateCloze(currentState) }
    }

    private fun regenerateCard(cardId: String) = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Card(cardId),
            actionName = "regenerateCard",
            failureTitle = "Couldn't regenerate the card",
            fallbackMessage = "Couldn't regenerate the card. Try again.",
            metadata = "cardId=$cardId",
        ) { previewWorkflow.regenerateCard(currentState, cardId) }
    }

    private fun regenerateField(field: EditableLearningNoteField) = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Field(field),
            actionName = "regenerateField",
            failureTitle = "Couldn't regenerate the field",
            fallbackMessage = "Couldn't regenerate the field. Try again.",
            metadata = "field=$field",
        ) { previewWorkflow.regenerateField(currentState, field) }
    }

    private suspend fun runPreviewWorkflowUpdate(
        target: PreviewRegenerationTarget,
        actionName: String,
        failureTitle: String,
        fallbackMessage: String,
        metadata: String = "",
        action: suspend () -> NewCardPreviewUpdateResult,
    ) {
        if (currentState.isLoading) return
        val preview = currentState.learningNotePreview ?: return
        val logContext = buildString {
            append("noteId=${preview.noteId}")
            if (metadata.isNotBlank()) append(" $metadata")
        }
        setState {
            copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = target,
            )
        }
        when (val result = action()) {
            is NewCardPreviewUpdateResult.Updated -> {
                setState { draftEditor.applyUpdatedPreview(this, result.preview) }
            }
            is NewCardPreviewUpdateResult.DomainError -> {
                setState {
                    copy(
                        error = NewCardErrorUi(
                            title = failureTitle,
                            validationIssues = result.issues,
                        ),
                        isLoading = false,
                        previewRegenerationTarget = null,
                    )
                }
            }
            is NewCardPreviewUpdateResult.UnexpectedError -> {
                val error = result.error
                logError(TAG, "$actionName:error $logContext ${error.message}", error)
                val classified = NewCardErrorClassifier.classifyRegenerationFailure(
                    error = error,
                    failureTitle = failureTitle,
                    fallbackMessage = fallbackMessage,
                )
                setState {
                    copy(
                        error = NewCardErrorUi(
                            title = classified.title,
                            message = classified.message,
                            quotaResetAt = classified.quotaResetAt,
                        ),
                        isLoading = false,
                        previewRegenerationTarget = null,
                    )
                }
            }
            NewCardPreviewUpdateResult.NoPreview -> {
                setState { copy(isLoading = false, previewRegenerationTarget = null) }
            }
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = currentState
        val deckId = current.deckSelected?.id ?: return@launch
        val learningNotePreview = current.learningNotePreview ?: return@launch
        val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(learningNotePreview)
        if (!previewValidation.isValid) {
            setState {
                withPreviewValidation(
                    preview = learningNotePreview,
                    validation = previewValidation,
                    error = null,
                )
            }
            return@launch
        }
        setState { copy(isLoading = true, error = null) }
        try {
            generationDependencies.createFlashcardUseCase(
                deckId = deckId,
                learningNote = learningNotePreview,
            )
            setState { resetAfterSave() }
            sendEffect(NewCardUiEffect.ShowMessage("Tarjeta creada"))
            sendEffect(NewCardUiEffect.CloseFlow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainValidationException) {
            setState {
                withPreviewValidation(
                    preview = learningNotePreview,
                    validation = previewValidation,
                    error = NewCardErrorUi(
                        title = "Preview no guardable",
                        validationIssues = e.issues,
                    ),
                )
            }
        } catch (e: Throwable) {
            logError(TAG, "saveFlashcard:error noteId=${learningNotePreview.noteId} ${e.message}", e)
            setState {
                copy(
                    error = NewCardErrorUi(
                        title = "Error al guardar",
                        message = "Couldn't save the card",
                    ),
                    isLoading = false,
                )
            }
            sendEffect(NewCardUiEffect.ShowMessage("Couldn't save the card"))
        }
    }
}

private const val TAG = "NewCardViewModel"
