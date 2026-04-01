package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.LearningDomain
import com.emm.domain.flashcard.LearningGoal
import com.emm.domain.flashcard.LevelBand
import com.emm.domain.flashcard.RegenerableNoteField
import com.emm.domain.flashcard.RegenerateLearningNoteClozeUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteExampleUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteFieldUseCase
import com.emm.domain.flashcard.RegenerateStudyCardUseCase
import com.emm.domain.flashcard.RegisterPreference
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.flashcard.ValidateGeneratedLearningNoteUseCase
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.logging.logInfo
import com.emm.hello.logging.logWarn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

private const val DEFAULT_COMMUNICATIVE_INTENT_ID = "social_small_talk"
private const val DESCRIBE_PAST_EVENTS_INTENT_ID = "describe_past_events"
private const val TALK_ABOUT_PLANS_INTENT_ID = "talk_about_plans"
private const val MAKE_POLITE_REQUESTS_INTENT_ID = "make_polite_requests"
private const val EXPRESS_EMOTIONS_INTENT_ID = "express_emotions"
private const val ORDER_FOOD_INTENT_ID = "order_food"
private const val ASK_FOR_DIRECTIONS_INTENT_ID = "ask_for_directions"
private const val HANDLE_COMPLAINTS_INTENT_ID = "handle_complaints"
private const val WORK_INTENT_ID = "work"
private const val SOLVE_DAILY_PROBLEMS_INTENT_ID = "solve_daily_problems"
private const val STUDY_INTENT_ID = "study"

private const val TRAVEL_CATEGORY_ID = 23
private const val WORK_CATEGORY_ID = 24
private const val DAILY_PROBLEMS_CATEGORY_ID = 25

private val socialSmallTalkCategoryIds = setOf(2, 8, 16, 22)
private val describePastEventsCategoryIds = setOf(4, 12, 13)
private val talkAboutPlansCategoryIds = setOf(14, 20)
private val makePoliteRequestsCategoryIds = setOf(15, 16, 17, 18)
private val expressEmotionsCategoryIds = setOf(21, 26)
private val studyCategoryIds = setOf(27, 28, 29, 30)
private val socialDomainCategoryIds = setOf(22, 26)

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val generateLearningNotePreviewUseCase: GenerateLearningNotePreviewUseCase,
    private val regenerateLearningNoteExampleUseCase: RegenerateLearningNoteExampleUseCase,
    private val regenerateLearningNoteClozeUseCase: RegenerateLearningNoteClozeUseCase,
    private val regenerateLearningNoteFieldUseCase: RegenerateLearningNoteFieldUseCase,
    private val regenerateStudyCardUseCase: RegenerateStudyCardUseCase,
    private val getDefaultDeckUseCase: GetDefaultDeckUseCase,
    private val setDefaultDeckUseCase: SetDefaultDeckUseCase,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
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
            is NewCardUiIntent.DeckSelected -> mutableState.update {
                it.copy(
                    deckSelected = intent.deck,
                    isCheck = getDefaultDeckUseCase() == intent.deck.id,
                )
            }
            is NewCardUiIntent.WordChanged -> mutableState.update {
                it.copy(
                    word = intent.word,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.AiRequestChanged -> mutableState.update {
                it.copy(
                    aiRequest = intent.aiRequest,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.IntendedMeaningChanged -> mutableState.update {
                it.copy(
                    intendedMeaningEs = intent.intendedMeaningEs,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.ContextSentenceChanged -> mutableState.update {
                it.copy(
                    contextSentence = intent.contextSentence,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.CheckChanged -> {
                val newDeckId = if (intent.checked) mutableState.value.deckSelected?.id.orEmpty() else ""
                setDefaultDeckUseCase(newDeckId)
                mutableState.update { it.copy(isCheck = intent.checked) }
            }
            NewCardUiIntent.GenerateClicked -> generateFlashcard()
            NewCardUiIntent.SaveClicked -> saveFlashcard()
            is NewCardUiIntent.CategorySelected -> mutableState.update {
                it.copy(
                    category = intent.category,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.DifficultySelected -> mutableState.update {
                it.copy(
                    difficulty = intent.difficulty,
                    error = null,
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.TypeViewSelected -> mutableState.update {
                it.copy(
                    typeView = intent.typeView,
                    learningNotePreview = null,
                    error = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            is NewCardUiIntent.PreviewFieldChanged -> updatePreview { note ->
                note.withEditedField(
                    field = intent.field,
                    value = intent.value,
                )
            }
            is NewCardUiIntent.PreviewCardPromptChanged -> updatePreview { note ->
                note.copy(
                    cards = note.cards.map { card ->
                        if (card.cardId == intent.cardId) card.copy(prompt = intent.prompt) else card
                    }
                )
            }
            is NewCardUiIntent.PreviewCardExpectedAnswerChanged -> updatePreview { note ->
                note.copy(
                    cards = note.cards.map { card ->
                        if (card.cardId == intent.cardId) {
                            card.copy(expectedAnswer = intent.expectedAnswer)
                        } else {
                            card
                        }
                    }
                )
            }
            is NewCardUiIntent.PreviewCardHintChanged -> updatePreview { note ->
                note.copy(
                    cards = note.cards.map { card ->
                        if (card.cardId == intent.cardId) card.copy(hint = intent.hint) else card
                    }
                )
            }
            is NewCardUiIntent.PreviewCardActiveChanged -> updatePreview { note ->
                note.copy(
                    cards = note.cards.map { card ->
                        if (card.cardId == intent.cardId) card.copy(isActive = intent.isActive) else card
                    }
                )
            }
            NewCardUiIntent.RegenerateExampleClicked -> regenerateExample()
            NewCardUiIntent.RegenerateClozeClicked -> regenerateCloze()
            is NewCardUiIntent.RegenerateFieldClicked -> regenerateField(intent.field)
            is NewCardUiIntent.RegenerateCardClicked -> regenerateCard(intent.cardId)
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        logInfo(
            TAG,
            "generateFlashcard:start typeView=${current.typeView} " +
                "deckId=${current.deckSelected?.id.orEmpty()}",
        )
        val inputValidation = validateInputUseCase(current.toGenerationInput())
        if (!inputValidation.isValid) {
            logWarn(
                TAG,
                "generateFlashcard:invalid_input " +
                    "firstError=${inputValidation.errors.firstOrNull()?.message.orEmpty()}",
            )
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Entrada inválida",
                        message = inputValidation.errors.firstOrNull()?.message
                            ?: "Corrige la entrada antes de generar.",
                    ),
                    learningNotePreview = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                )
            }
            return@launch
        }
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                learningNotePreview = null,
                previewValidationErrors = emptyList(),
                previewWarnings = emptyList(),
                previewValidationIssues = emptyList(),
                previewWarningIssues = emptyList(),
                canSavePreview = false,
            )
        }
        mutableEffect.send(NewCardUiEffect.OpenReview)
        runCatching {
            generateLearningNotePreviewUseCase(
                input = inputValidation.normalizedInput
            )
        }.onSuccess { preview ->
            val previewValidation = validateGeneratedLearningNoteUseCase(preview)
            logInfo(
                TAG,
                "generateFlashcard:success noteId=${preview.noteId} cards=${preview.cards.size} errors=${previewValidation.errors.size} warnings=${previewValidation.warnings.size}"
            )
            mutableState.update {
                it.copy(
                    learningNotePreview = preview,
                    previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                    previewWarnings = previewValidation.warnings.map { issue -> issue.message } + preview.warnings,
                    previewValidationIssues = previewValidation.errors,
                    previewWarningIssues = previewValidation.warnings,
                    canSavePreview = previewValidation.isValid,
                    isLoading = false,
                    previewRegenerationTarget = null,
                )
            }
        }.onFailure { e ->
            logError(TAG, "generateFlashcard:error ${e.message}", e)
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Respuesta inválida de IA",
                        message = e.message ?: "No se pudo generar una learning note válida.",
                    ),
                    isLoading = false,
                    canSavePreview = false,
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    previewRegenerationTarget = null,
                )
            }
        }
    }

    private fun regenerateExample() = viewModelScope.launch {
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return@launch
        logInfo(TAG, "regenerateExample:start noteId=${preview.noteId}")
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = PreviewRegenerationTarget.Example,
            )
        }
        runCatching {
            regenerateLearningNoteExampleUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
        }.onSuccess { example ->
            logInfo(TAG, "regenerateExample:success noteId=${preview.noteId}")
            applyUpdatedPreview(
                preview.copy(
                    exampleSentence = example.sentence,
                    exampleTranslation = example.translation,
                )
            )
        }.onFailure { e ->
            logError(TAG, "regenerateExample:error ${e.message}", e)
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Error al regenerar ejemplo",
                        message = e.message ?: "No se pudo regenerar el ejemplo.",
                    ),
                    isLoading = false,
                    previewRegenerationTarget = null,
                )
            }
        }
    }

    private fun regenerateCloze() = viewModelScope.launch {
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return@launch
        logInfo(TAG, "regenerateCloze:start noteId=${preview.noteId}")
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = PreviewRegenerationTarget.Cloze,
            )
        }
        runCatching {
            regenerateLearningNoteClozeUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
        }.onSuccess { cloze ->
            logInfo(TAG, "regenerateCloze:success noteId=${preview.noteId}")
            applyUpdatedPreview(preview.copy(clozeSentence = cloze))
        }.onFailure { e ->
            logError(TAG, "regenerateCloze:error ${e.message}", e)
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Error al regenerar cloze",
                        message = e.message ?: "No se pudo regenerar el cloze.",
                    ),
                    isLoading = false,
                    previewRegenerationTarget = null,
                )
            }
        }
    }

    private fun regenerateCard(cardId: String) = viewModelScope.launch {
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return@launch
        logInfo(TAG, "regenerateCard:start noteId=${preview.noteId} cardId=$cardId")
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = PreviewRegenerationTarget.Card(cardId),
            )
        }
        runCatching {
            regenerateStudyCardUseCase(
                input = current.toGenerationInput(),
                note = preview,
                cardId = cardId,
            )
        }.onSuccess { regeneratedCard ->
            logInfo(TAG, "regenerateCard:success noteId=${preview.noteId} cardId=$cardId")
            applyUpdatedPreview(
                preview.copy(
                    cards = preview.cards.map { card ->
                        if (card.cardId == cardId) regeneratedCard else card
                    }
                )
            )
        }.onFailure { e ->
            logError(TAG, "regenerateCard:error cardId=$cardId ${e.message}", e)
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Error al regenerar card",
                        message = e.message ?: "No se pudo regenerar la card.",
                    ),
                    isLoading = false,
                    previewRegenerationTarget = null,
                )
            }
        }
    }

    private fun regenerateField(field: EditableLearningNoteField) = viewModelScope.launch {
        val regenerableField = field.toRegenerableFieldOrNull() ?: return@launch
        val current = mutableState.value
        val preview = current.learningNotePreview ?: return@launch
        logInfo(TAG, "regenerateField:start noteId=${preview.noteId} field=$field")
        mutableState.update {
            it.copy(
                isLoading = true,
                error = null,
                previewRegenerationTarget = PreviewRegenerationTarget.Field(field),
            )
        }
        runCatching {
            regenerateLearningNoteFieldUseCase(
                input = current.toGenerationInput(),
                note = preview,
                field = regenerableField,
            )
        }.onSuccess { value ->
            logInfo(TAG, "regenerateField:success noteId=${preview.noteId} field=$field")
            val updatedPreview = when (field) {
                EditableLearningNoteField.WhyUseful -> preview.copy(whyUseful = value)
                EditableLearningNoteField.UsagePattern -> preview.copy(usagePattern = value)
                EditableLearningNoteField.CommonMistake -> preview.copy(commonMistake = value)
                else -> preview
            }
            applyUpdatedPreview(updatedPreview)
        }.onFailure { e ->
            logError(TAG, "regenerateField:error field=$field ${e.message}", e)
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Error al regenerar campo",
                        message = e.message ?: "No se pudo regenerar el campo.",
                    ),
                    isLoading = false,
                    previewRegenerationTarget = null,
                )
            }
        }
    }

    private fun updatePreview(transform: (GeneratedLearningNote) -> GeneratedLearningNote) {
        val currentPreview = mutableState.value.learningNotePreview ?: return
        applyUpdatedPreview(transform(currentPreview))
    }

    private fun applyUpdatedPreview(updatedPreview: GeneratedLearningNote) {
        val previewValidation = validateGeneratedLearningNoteUseCase(updatedPreview)
        logInfo(
            TAG,
            "applyUpdatedPreview noteId=${updatedPreview.noteId} cards=${updatedPreview.cards.size} errors=${previewValidation.errors.size} warnings=${previewValidation.warnings.size}"
        )
        mutableState.update {
            it.copy(
                learningNotePreview = updatedPreview,
                error = null,
                previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                previewWarnings = previewValidation.warnings.map { issue -> issue.message } + updatedPreview.warnings,
                previewValidationIssues = previewValidation.errors,
                previewWarningIssues = previewValidation.warnings,
                canSavePreview = previewValidation.isValid,
                isLoading = false,
                previewRegenerationTarget = null,
            )
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        val deckId = current.deckSelected?.id ?: return@launch
        val learningNotePreview = current.learningNotePreview ?: return@launch
        logInfo(TAG, "saveFlashcard:start deckId=$deckId noteId=${learningNotePreview.noteId}")
        val previewValidation = validateGeneratedLearningNoteUseCase(learningNotePreview)
        if (!previewValidation.isValid) {
            logWarn(
                TAG,
                "saveFlashcard:blocked_invalid_preview " +
                    "firstError=${previewValidation.errors.firstOrNull()?.message.orEmpty()}",
            )
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Preview no guardable",
                        message = previewValidation.errors.firstOrNull()?.message
                            ?: "La learning note no cumple las validaciones necesarias.",
                    ),
                    previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                    previewWarnings = previewValidation.warnings.map { issue -> issue.message } + learningNotePreview.warnings,
                    previewValidationIssues = previewValidation.errors,
                    previewWarningIssues = previewValidation.warnings,
                    canSavePreview = false,
                    previewRegenerationTarget = null,
                )
            }
            return@launch
        }
        mutableState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            createFlashcardUseCase(
                deckId = deckId,
                learningNote = learningNotePreview,
            )
        }.onSuccess {
            logInfo(TAG, "saveFlashcard:success deckId=$deckId noteId=${learningNotePreview.noteId}")
            mutableState.update {
                it.copy(
                    word = "",
                    aiRequest = "",
                    intendedMeaningEs = "",
                    contextSentence = "",
                    learningNotePreview = null,
                    isLoading = false,
                    error = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    previewValidationIssues = emptyList(),
                    previewWarningIssues = emptyList(),
                    canSavePreview = false,
                    previewRegenerationTarget = null,
                )
            }
            mutableEffect.send(NewCardUiEffect.ShowMessage("Tarjeta creada"))
            mutableEffect.send(NewCardUiEffect.CloseFlow)
        }.onFailure { e ->
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

    private fun NewCardUiState.toGenerationInput(): FlashcardGenerationInput {
        return when (typeView) {
            TypeView.WordOrPhase -> FlashcardGenerationInput(
                inputType = word.inferInputType(),
                userText = word,
                intendedMeaningEs = intendedMeaningEs,
                contextSentence = contextSentence,
                learningGoal = LearningGoal.Both,
                levelBand = difficulty.toLevelBand(),
                register = RegisterPreference.Neutral,
                domain = LearningDomain.DailyLife,
            )

            TypeView.WithCategories -> FlashcardGenerationInput(
                inputType = FlashcardInputType.CommunicativeGoal,
                userText = category.toGenerationPrompt(),
                learningGoal = LearningGoal.Both,
                levelBand = difficulty.toLevelBand(),
                register = RegisterPreference.Neutral,
                domain = category.toLearningDomain(),
                communicativeIntentId = category.toCommunicativeIntentId(),
            )

            TypeView.WithAiHelp -> FlashcardGenerationInput(
                inputType = FlashcardInputType.CommunicativeGoal,
                userText = aiRequest,
                learningGoal = LearningGoal.Both,
                levelBand = difficulty.toLevelBand(),
                register = RegisterPreference.Neutral,
                domain = aiRequest.toLearningDomain(),
                communicativeIntentId = aiRequest.toCommunicativeIntentId(),
            )
        }
    }

    private fun String.inferInputType(): FlashcardInputType {
        val trimmed = trim()
        return when {
            trimmed.contains("?") || trimmed.contains(".") || trimmed.contains("!") -> FlashcardInputType.Sentence
            trimmed.contains(" ") -> FlashcardInputType.Phrase
            else -> FlashcardInputType.Word
        }
    }

    private fun String.toLevelBand(): LevelBand {
        return when (lowercase()) {
            "intermedio" -> LevelBand.B1_B2
            "avanzado" -> LevelBand.C1_PLUS
            else -> LevelBand.A1_A2
        }
    }

    private fun StaticCategories.toGenerationPrompt(): String {
        return "Create a high-value English learning note for the category: $name"
    }

    private fun StaticCategories.toCommunicativeIntentId(): String {
        return when (id) {
            in socialSmallTalkCategoryIds -> DEFAULT_COMMUNICATIVE_INTENT_ID
            in describePastEventsCategoryIds -> DESCRIBE_PAST_EVENTS_INTENT_ID
            in talkAboutPlansCategoryIds -> TALK_ABOUT_PLANS_INTENT_ID
            in makePoliteRequestsCategoryIds -> MAKE_POLITE_REQUESTS_INTENT_ID
            in expressEmotionsCategoryIds -> EXPRESS_EMOTIONS_INTENT_ID
            TRAVEL_CATEGORY_ID -> ASK_FOR_DIRECTIONS_INTENT_ID
            WORK_CATEGORY_ID -> WORK_INTENT_ID
            DAILY_PROBLEMS_CATEGORY_ID -> SOLVE_DAILY_PROBLEMS_INTENT_ID
            in studyCategoryIds -> STUDY_INTENT_ID
            else -> DEFAULT_COMMUNICATIVE_INTENT_ID
        }
    }

    private fun StaticCategories.toLearningDomain(): LearningDomain {
        return when (id) {
            TRAVEL_CATEGORY_ID -> LearningDomain.Travel
            WORK_CATEGORY_ID -> LearningDomain.Work
            in studyCategoryIds -> LearningDomain.Study
            in socialDomainCategoryIds -> LearningDomain.Social
            else -> LearningDomain.DailyLife
        }
    }

    private fun String.toCommunicativeIntentId(): String {
        val normalized = normalizeForInference()
        return when {
            normalized.hasAnyKeyword(
                "restaurante",
                "restaurant",
                "comida",
                "food",
                "menu",
                "cafeteria",
                "cafe",
                "delivery",
                "ordenar",
                "pedir",
                "order",
            ) -> ORDER_FOOD_INTENT_ID
            normalized.hasAnyKeyword(
                "aeropuerto",
                "airport",
                "vuelo",
                "flight",
                "hotel",
                "taxi",
                "check in",
                "boarding",
                "gate",
                "travel",
                "viaje",
            ) -> ASK_FOR_DIRECTIONS_INTENT_ID
            normalized.hasAnyKeyword(
                "trabajo",
                "work",
                "oficina",
                "office",
                "negocio",
                "business",
                "reunion",
                "meeting",
                "entrevista",
                "interview",
                "email",
                "correo",
            ) -> WORK_INTENT_ID
            normalized.hasAnyKeyword(
                "estudio",
                "study",
                "clase",
                "class",
                "escuela",
                "school",
                "universidad",
                "university",
                "examen",
                "exam",
            ) -> STUDY_INTENT_ID
            normalized.hasAnyKeyword(
                "queja",
                "complaint",
                "reclamo",
                "refund",
                "devolucion",
                "customer service",
                "servicio al cliente",
            ) -> HANDLE_COMPLAINTS_INTENT_ID
            normalized.hasAnyKeyword(
                "ayuda",
                "help",
                "please",
                "favor",
                "request",
                "solicitar",
                "pedir ayuda",
            ) -> MAKE_POLITE_REQUESTS_INTENT_ID
            normalized.hasAnyKeyword(
                "emocion",
                "emotion",
                "sentimiento",
                "feeling",
                "feliz",
                "happy",
                "triste",
                "sad",
                "frustrado",
                "angry",
            ) -> EXPRESS_EMOTIONS_INTENT_ID
            normalized.hasAnyKeyword(
                "pasado",
                "past",
                "ayer",
                "experiencia",
                "experience",
                "historia",
                "story",
            ) -> DESCRIBE_PAST_EVENTS_INTENT_ID
            normalized.hasAnyKeyword(
                "planes",
                "future",
                "futuro",
                "mañana",
                "tomorrow",
                "plan",
                "going to",
                "next week",
            ) -> TALK_ABOUT_PLANS_INTENT_ID
            else -> DEFAULT_COMMUNICATIVE_INTENT_ID
        }
    }

    private fun String.toLearningDomain(): LearningDomain {
        val normalized = normalizeForInference()
        return when {
            normalized.hasAnyKeyword(
                "aeropuerto",
                "airport",
                "hotel",
                "taxi",
                "travel",
                "viaje",
                "vacaciones",
                "vacation",
                "boarding",
                "flight",
            ) -> LearningDomain.Travel
            normalized.hasAnyKeyword(
                "trabajo",
                "work",
                "oficina",
                "office",
                "negocio",
                "business",
                "cliente",
                "client",
            ) -> LearningDomain.Work
            normalized.hasAnyKeyword(
                "amigos",
                "friends",
                "social",
                "party",
                "cita",
                "date",
                "conversation",
                "conversacion",
            ) -> LearningDomain.Social
            normalized.hasAnyKeyword(
                "estudio",
                "study",
                "escuela",
                "school",
                "universidad",
                "university",
                "exam",
                "examen",
            ) -> LearningDomain.Study
            else -> LearningDomain.DailyLife
        }
    }
}

private const val TAG = "NewCardViewModel"

private fun String.normalizeForInference(): String {
    return Normalizer
        .normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}

private fun String.hasAnyKeyword(vararg keywords: String): Boolean {
    return keywords.any { keyword -> contains(keyword) }
}

private fun EditableLearningNoteField.toRegenerableFieldOrNull(): RegenerableNoteField? {
    return when (this) {
        EditableLearningNoteField.WhyUseful -> RegenerableNoteField.WhyUseful
        EditableLearningNoteField.UsagePattern -> RegenerableNoteField.UsagePattern
        EditableLearningNoteField.CommonMistake -> RegenerableNoteField.CommonMistake
        else -> null
    }
}

private fun GeneratedLearningNote.withEditedField(
    field: EditableLearningNoteField,
    value: String,
): GeneratedLearningNote {
    return when (field) {
        EditableLearningNoteField.IntendedMeaningEs -> copy(intendedMeaningEs = value)
        EditableLearningNoteField.SimpleDefinitionEn -> copy(simpleDefinitionEn = value)
        EditableLearningNoteField.WhyUseful -> copy(whyUseful = value)
        EditableLearningNoteField.ExampleSentence -> copy(exampleSentence = value)
        EditableLearningNoteField.ExampleTranslation -> copy(exampleTranslation = value)
        EditableLearningNoteField.UsagePattern -> copy(usagePattern = value)
        EditableLearningNoteField.CommonMistake -> copy(commonMistake = value)
        EditableLearningNoteField.ClozeSentence -> copy(clozeSentence = value)
    }
}
