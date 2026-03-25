package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.LearningDomain
import com.emm.domain.flashcard.LearningGoal
import com.emm.domain.flashcard.LevelBand
import com.emm.domain.flashcard.RegisterPreference
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.flashcard.ValidateGeneratedLearningNoteUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_COMMUNICATIVE_INTENT_ID = "social_small_talk"
private const val DESCRIBE_PAST_EVENTS_INTENT_ID = "describe_past_events"
private const val TALK_ABOUT_PLANS_INTENT_ID = "talk_about_plans"
private const val MAKE_POLITE_REQUESTS_INTENT_ID = "make_polite_requests"
private const val EXPRESS_EMOTIONS_INTENT_ID = "express_emotions"
private const val ASK_FOR_DIRECTIONS_INTENT_ID = "ask_for_directions"
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
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        val inputValidation = validateInputUseCase(current.toGenerationInput())
        if (!inputValidation.isValid) {
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
                canSavePreview = false,
            )
        }
        runCatching {
            generateLearningNotePreviewUseCase(
                input = inputValidation.normalizedInput
            )
        }.onSuccess { preview ->
            val previewValidation = validateGeneratedLearningNoteUseCase(preview)
            mutableState.update {
                it.copy(
                    learningNotePreview = preview,
                    previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                    previewWarnings = previewValidation.warnings.map { issue -> issue.message } + preview.warnings,
                    canSavePreview = previewValidation.isValid,
                    isLoading = false,
                )
            }
        }.onFailure { e ->
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Respuesta inválida de IA",
                        message = e.message ?: "No se pudo generar una learning note válida.",
                    ),
                    isLoading = false,
                    canSavePreview = false,
                )
            }
        }
    }

    private fun updatePreview(transform: (GeneratedLearningNote) -> GeneratedLearningNote) {
        val currentPreview = mutableState.value.learningNotePreview ?: return
        val updatedPreview = transform(currentPreview)
        val previewValidation = validateGeneratedLearningNoteUseCase(updatedPreview)
        mutableState.update {
            it.copy(
                learningNotePreview = updatedPreview,
                error = null,
                previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                previewWarnings = previewValidation.warnings.map { issue -> issue.message } + updatedPreview.warnings,
                canSavePreview = previewValidation.isValid,
            )
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        val deckId = current.deckSelected?.id ?: return@launch
        val learningNotePreview = current.learningNotePreview
        if (learningNotePreview == null) return@launch
        val previewValidation = validateGeneratedLearningNoteUseCase(learningNotePreview)
        if (!previewValidation.isValid) {
            mutableState.update {
                it.copy(
                    error = NewCardErrorUi(
                        title = "Preview no guardable",
                        message = previewValidation.errors.firstOrNull()?.message
                            ?: "La learning note no cumple las validaciones necesarias.",
                    ),
                    previewValidationErrors = previewValidation.errors.map { issue -> issue.message },
                    previewWarnings = previewValidation.warnings.map { issue -> issue.message } + learningNotePreview.warnings,
                    canSavePreview = false,
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
            mutableState.update {
                it.copy(
                    word = "",
                    intendedMeaningEs = "",
                    contextSentence = "",
                    learningNotePreview = null,
                    isLoading = false,
                    error = null,
                    previewValidationErrors = emptyList(),
                    previewWarnings = emptyList(),
                    canSavePreview = false,
                )
            }
            mutableEffect.send(NewCardUiEffect.ShowMessage("Tarjeta creada"))
        }.onFailure { e ->
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
