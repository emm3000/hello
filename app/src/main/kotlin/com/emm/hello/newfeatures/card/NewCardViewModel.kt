package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedStudyCard
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
    private val getDefaultDeckUseCase: GetDefaultDeckUseCase,
    private val setDefaultDeckUseCase: SetDefaultDeckUseCase,
) : MviViewModel<NewCardUiState, NewCardUiIntent, NewCardUiEffect>(
    initialState = NewCardUiState(),
) {

    init {
        getDecksUseCase()
            .onEach { decks ->
                val defaultDeckId = getDefaultDeckUseCase()
                val selectedDeck = decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
                mutableState.update {
                    it.copy(
                        decks = decks,
                        deckSelected = selectedDeck,
                        isCheck = defaultDeckId.isNotEmpty() && selectedDeck?.id == defaultDeckId
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
                    isCheck = getDefaultDeckUseCase() == intent.deck.id,
                )
            }
            is NewCardUiIntent.CheckChanged -> {
                val newDeckId = if (intent.checked) mutableState.value.deckSelected?.id.orEmpty() else ""
                setDefaultDeckUseCase(newDeckId)
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
            is NewCardUiIntent.PreviewFieldChanged -> updatePreview { note ->
                note.withEditedField(field = intent.field, value = intent.value)
            }
            is NewCardUiIntent.PreviewCardPromptChanged -> updatePreviewCard(intent.cardId) {
                copy(prompt = intent.prompt)
            }
            is NewCardUiIntent.PreviewCardExpectedAnswerChanged -> updatePreviewCard(intent.cardId) {
                copy(expectedAnswer = intent.expectedAnswer)
            }
            is NewCardUiIntent.PreviewCardHintChanged -> updatePreviewCard(intent.cardId) {
                copy(hint = intent.hint)
            }
            is NewCardUiIntent.PreviewCardActiveChanged -> updatePreviewCard(intent.cardId) {
                copy(isActive = intent.isActive)
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
        val current = mutableState.value
        val inputValidation = generationDependencies.validateInputUseCase(current.toGenerationInput())
        if (!inputValidation.isValid) {
            mutableState.update {
                it.clearPreviewState(
                    error = null,
                ).copy(
                    inputValidationIssues = inputValidation.errors,
                    inputWarningIssues = inputValidation.warnings,
                )
            }
            return@launch
        }
        mutableState.update { it.clearPreviewState(error = null, isLoading = true) }
        mutableEffect.send(NewCardUiEffect.OpenReview)
        runCatching {
            generationDependencies.generateLearningNotePreviewUseCase(
                input = inputValidation.value
            )
        }.onSuccess { preview ->
            val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(preview)
            mutableState.update { it.withPreviewValidation(preview, previewValidation) }
        }.onFailure { e ->
            when (e) {
                is DomainValidationException -> {
                    mutableState.update {
                        it.clearPreviewState(
                            error = NewCardErrorUi(
                                title = "Respuesta inválida de IA",
                                validationIssues = e.issues,
                            ),
                        )
                    }
                }
                else -> {
                    logError(TAG, "generateFlashcard:error ${e.message}", e)
                    mutableState.update {
                        it.copy(
                            error = NewCardErrorUi(
                                title = "Respuesta inválida de IA",
                                message = e.message ?: "No se pudo generar una learning note válida.",
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
    }

    private fun regenerateExample() = viewModelScope.launch {
        runPreviewUpdate(
            target = PreviewRegenerationTarget.Example,
            actionName = "regenerateExample",
            failureTitle = "Error al regenerar ejemplo",
            fallbackMessage = "No se pudo regenerar el ejemplo.",
        ) { current, preview ->
            val example = generationDependencies.regenerateLearningNoteExampleUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
            preview.copy(
                exampleSentence = example.sentence,
                exampleTranslation = example.translation,
            )
        }
    }

    private fun regenerateCloze() = viewModelScope.launch {
        runPreviewUpdate(
            target = PreviewRegenerationTarget.Cloze,
            actionName = "regenerateCloze",
            failureTitle = "Error al regenerar cloze",
            fallbackMessage = "No se pudo regenerar el cloze.",
        ) { current, preview ->
            val cloze = generationDependencies.regenerateLearningNoteClozeUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
            preview.copy(clozeSentence = cloze)
        }
    }

    private fun regenerateCard(cardId: String) = viewModelScope.launch {
        runPreviewUpdate(
            target = PreviewRegenerationTarget.Card(cardId),
            actionName = "regenerateCard",
            failureTitle = "Error al regenerar card",
            fallbackMessage = "No se pudo regenerar la card.",
            metadata = "cardId=$cardId",
        ) { current, preview ->
            val regeneratedCard = generationDependencies.regenerateStudyCardUseCase(
                input = current.toGenerationInput(),
                note = preview,
                cardId = cardId,
            )
            preview.copy(
                cards = preview.cards.map { card ->
                    if (card.cardId == cardId) regeneratedCard else card
                }
            )
        }
    }

    private fun regenerateField(field: EditableLearningNoteField) = viewModelScope.launch {
        val regenerableField = field.toRegenerableFieldOrNull() ?: return@launch
        runPreviewUpdate(
            target = PreviewRegenerationTarget.Field(field),
            actionName = "regenerateField",
            failureTitle = "Error al regenerar campo",
            fallbackMessage = "No se pudo regenerar el campo.",
            metadata = "field=$field",
        ) { current, preview ->
            val value = generationDependencies.regenerateLearningNoteFieldUseCase(
                input = current.toGenerationInput(),
                note = preview,
                field = regenerableField,
            )
            when (field) {
                EditableLearningNoteField.WhyUseful -> preview.copy(whyUseful = value)
                EditableLearningNoteField.UsagePattern -> preview.copy(usagePattern = value)
                EditableLearningNoteField.CommonMistake -> preview.copy(commonMistake = value)
                else -> preview
            }
        }
    }

    private fun runPreviewUpdate(
        target: PreviewRegenerationTarget,
        actionName: String,
        failureTitle: String,
        fallbackMessage: String,
        metadata: String = "",
        transform: suspend (current: NewCardUiState, preview: GeneratedLearningNote) -> GeneratedLearningNote,
    ) = viewModelScope.launch {
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return@launch
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
        runCatching {
            transform(current, preview)
        }.onSuccess { updatedPreview ->
            applyUpdatedPreview(updatedPreview)
        }.onFailure { e ->
            if (e is DomainValidationException) {
                mutableState.update {
                    it.copy(
                        error = NewCardErrorUi(
                            title = failureTitle,
                            validationIssues = e.issues,
                        ),
                        isLoading = false,
                        previewRegenerationTarget = null,
                    )
                }
            } else {
                logError(TAG, "$actionName:error $logContext ${e.message}", e)
                mutableState.update {
                    it.copy(
                        error = NewCardErrorUi(
                            title = failureTitle,
                            message = e.message ?: fallbackMessage,
                        ),
                        isLoading = false,
                        previewRegenerationTarget = null,
                    )
                }
            }
        }
    }

    private fun updatePreview(transform: (GeneratedLearningNote) -> GeneratedLearningNote) {
        val currentPreview = mutableState.value.learningNotePreview ?: return
        applyUpdatedPreview(transform(currentPreview))
    }

    private fun updatePreviewCard(
        cardId: String,
        transform: GeneratedStudyCard.() -> GeneratedStudyCard,
    ) {
        updatePreview { note ->
            note.copy(
                cards = note.cards.map { card ->
                    if (card.cardId == cardId) card.transform() else card
                }
            )
        }
    }

    private fun applyUpdatedPreview(updatedPreview: GeneratedLearningNote) {
        val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(updatedPreview)
        mutableState.update { it.withPreviewValidation(updatedPreview, previewValidation) }
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
