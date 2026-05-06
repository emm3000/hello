package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.validation.DomainValidationException
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val generationDependencies: NewCardGenerationDependencies,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
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
                mutableState.update {
                    it.copy(
                        decks = decks,
                        deckSelected = selectedDeck,
                        isCheck = defaultDeckId != null && selectedDeck?.id == defaultDeckId
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.DeckSelected,
            is NewCardUiIntent.CheckChanged -> handleDeckIntent(intent)
            is NewCardUiIntent.WordChanged,
            is NewCardUiIntent.AiRequestChanged,
            is NewCardUiIntent.IntendedMeaningChanged,
            is NewCardUiIntent.ContextSentenceChanged,
            is NewCardUiIntent.CategorySelected,
            is NewCardUiIntent.DifficultySelected,
            is NewCardUiIntent.TypeViewSelected -> handleInputIntent(intent)
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
            is NewCardUiIntent.DeckSelected -> mutableState.update {
                it.copy(
                    deckSelected = intent.deck,
                    isCheck = defaultDeckSelectionRepository.getDefaultDeckId() == intent.deck.id,
                )
            }
            is NewCardUiIntent.CheckChanged -> {
                val newDeckId = if (intent.checked) mutableState.value.deckSelected?.id else null
                defaultDeckSelectionRepository.setDefaultDeckId(newDeckId)
                mutableState.update { it.copy(isCheck = intent.checked) }
            }
            else -> Unit
        }
    }

    private fun handleInputIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.WordChanged -> updateInputState { copy(word = intent.word) }
            is NewCardUiIntent.AiRequestChanged -> updateInputState { copy(aiRequest = intent.aiRequest) }
            is NewCardUiIntent.IntendedMeaningChanged -> {
                updateInputState { copy(intendedMeaningEs = intent.intendedMeaningEs) }
            }
            is NewCardUiIntent.ContextSentenceChanged -> {
                updateInputState { copy(contextSentence = intent.contextSentence) }
            }
            is NewCardUiIntent.CategorySelected -> updateInputState { copy(category = intent.category) }
            is NewCardUiIntent.DifficultySelected -> updateInputState { copy(difficulty = intent.difficulty) }
            is NewCardUiIntent.TypeViewSelected -> updateInputState { copy(typeView = intent.typeView) }
            else -> Unit
        }
    }

    private fun handlePreviewEditIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.PreviewFieldChanged -> mutableState.update {
                draftEditor.editField(it, intent.field, intent.value)
            }
            is NewCardUiIntent.PreviewCardPromptChanged -> mutableState.update {
                draftEditor.editCardPrompt(it, intent.cardId, intent.prompt)
            }
            is NewCardUiIntent.PreviewCardExpectedAnswerChanged -> mutableState.update {
                draftEditor.editCardExpectedAnswer(it, intent.cardId, intent.expectedAnswer)
            }
            is NewCardUiIntent.PreviewCardHintChanged -> mutableState.update {
                draftEditor.editCardHint(it, intent.cardId, intent.hint)
            }
            is NewCardUiIntent.PreviewCardActiveChanged -> mutableState.update {
                draftEditor.editCardActive(it, intent.cardId, intent.isActive)
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
        mutableState.update { current -> transform(current).clearPreviewState(error = null) }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        mutableState.update { it.clearPreviewState(error = null, isLoading = true) }
        mutableEffect.send(NewCardUiEffect.OpenReview)

        when (val result = previewWorkflow.generate(mutableState.value)) {
            is NewCardPreviewResult.InputInvalid -> {
                mutableState.update {
                    it.clearPreviewState(error = null).copy(
                        inputValidationIssues = result.errors,
                        inputWarningIssues = result.warnings,
                    )
                }
            }
            is NewCardPreviewResult.PreviewReady -> {
                mutableState.update { it.withPreviewValidation(result.preview, result.validation) }
            }
            is NewCardPreviewResult.DomainError -> {
                mutableState.update {
                    it.clearPreviewState(
                        error = NewCardErrorUi(
                            title = "Respuesta inválida de IA",
                            validationIssues = result.issues,
                        ),
                    )
                }
            }
            is NewCardPreviewResult.UnexpectedError -> {
                val error = result.error
                logError(TAG, "generateFlashcard:error ${error.message}", error)
                mutableState.update {
                    it.copy(
                        error = NewCardErrorUi(
                            title = "Respuesta inválida de IA",
                            message = error.message ?: "No se pudo generar una learning note válida.",
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
            failureTitle = "Error al regenerar ejemplo",
            fallbackMessage = "No se pudo regenerar el ejemplo.",
        ) { previewWorkflow.regenerateExample(mutableState.value) }
    }

    private fun regenerateCloze() = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Cloze,
            actionName = "regenerateCloze",
            failureTitle = "Error al regenerar cloze",
            fallbackMessage = "No se pudo regenerar el cloze.",
        ) { previewWorkflow.regenerateCloze(mutableState.value) }
    }

    private fun regenerateCard(cardId: String) = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Card(cardId),
            actionName = "regenerateCard",
            failureTitle = "Error al regenerar card",
            fallbackMessage = "No se pudo regenerar la card.",
            metadata = "cardId=$cardId",
        ) { previewWorkflow.regenerateCard(mutableState.value, cardId) }
    }

    private fun regenerateField(field: EditableLearningNoteField) = viewModelScope.launch {
        runPreviewWorkflowUpdate(
            target = PreviewRegenerationTarget.Field(field),
            actionName = "regenerateField",
            failureTitle = "Error al regenerar campo",
            fallbackMessage = "No se pudo regenerar el campo.",
            metadata = "field=$field",
        ) { previewWorkflow.regenerateField(mutableState.value, field) }
    }

    private suspend fun runPreviewWorkflowUpdate(
        target: PreviewRegenerationTarget,
        actionName: String,
        failureTitle: String,
        fallbackMessage: String,
        metadata: String = "",
        action: suspend () -> NewCardPreviewUpdateResult,
    ) {
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return
        val logContext = buildString {
            append("noteId=${preview.noteId}")
            if (metadata.isNotBlank()) append(" $metadata")
        }
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = target,
            )
        }
        when (val result = action()) {
            is NewCardPreviewUpdateResult.Updated -> {
                mutableState.update {
                    draftEditor.applyUpdatedPreview(it, result.preview)
                }
            }
            is NewCardPreviewUpdateResult.DomainError -> {
                mutableState.update {
                    it.copy(
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
                mutableState.update {
                    it.copy(
                        error = NewCardErrorUi(
                            title = failureTitle,
                            message = error.message ?: fallbackMessage,
                        ),
                        isLoading = false,
                        previewRegenerationTarget = null,
                    )
                }
            }
            NewCardPreviewUpdateResult.NoPreview -> {
                mutableState.update {
                    it.copy(isLoading = false, previewRegenerationTarget = null)
                }
            }
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        val deckId = current.deckSelected?.id ?: return@launch
        val learningNotePreview = current.learningNotePreview ?: return@launch
        val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(learningNotePreview)
        if (!previewValidation.isValid) {
            mutableState.update {
                it.withPreviewValidation(
                    preview = learningNotePreview,
                    validation = previewValidation,
                    error = null,
                )
            }
            return@launch
        }
        mutableState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            generationDependencies.createFlashcardUseCase(
                deckId = deckId,
                learningNote = learningNotePreview,
            )
        }.onSuccess {
            mutableState.update(NewCardUiState::resetAfterSave)
            mutableEffect.send(NewCardUiEffect.ShowMessage("Tarjeta creada"))
            mutableEffect.send(NewCardUiEffect.CloseFlow)
        }.onFailure { e ->
            if (e is DomainValidationException) {
                mutableState.update {
                    it.withPreviewValidation(
                        preview = learningNotePreview,
                        validation = previewValidation,
                        error = NewCardErrorUi(
                            title = "Preview no guardable",
                            validationIssues = e.issues,
                        ),
                    )
                }
            } else {
                logError(TAG, "saveFlashcard:error noteId=${learningNotePreview.noteId} ${e.message}", e)
                mutableState.update {
                    it.copy(
                        error = NewCardErrorUi(
                            title = "Error al guardar",
                            message = e.message ?: "No se pudo guardar la tarjeta",
                        ),
                        isLoading = false,
                    )
                }
                mutableEffect.send(NewCardUiEffect.ShowMessage(e.message ?: "No se pudo guardar la tarjeta"))
            }
        }
    }
}

private const val TAG = "NewCardViewModel"
