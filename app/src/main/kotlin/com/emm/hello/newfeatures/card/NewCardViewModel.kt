package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardGenerated
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.GenerateFlashcardPreviewUseCase
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.LearningDomain
import com.emm.domain.flashcard.LearningGoal
import com.emm.domain.flashcard.LevelBand
import com.emm.domain.flashcard.RegisterPreference
import com.emm.domain.flashcard.TypeView
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val generateFlashcardPreviewUseCase: GenerateFlashcardPreviewUseCase,
    private val generateLearningNotePreviewUseCase: GenerateLearningNotePreviewUseCase,
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
                    previewResult = null
                )
            }
            is NewCardUiIntent.IntendedMeaningChanged -> mutableState.update {
                it.copy(
                    intendedMeaningEs = intent.intendedMeaningEs,
                    error = null,
                    learningNotePreview = null,
                    previewResult = null,
                )
            }
            is NewCardUiIntent.ContextSentenceChanged -> mutableState.update {
                it.copy(
                    contextSentence = intent.contextSentence,
                    error = null,
                    learningNotePreview = null,
                    previewResult = null,
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
                    previewResult = null
                )
            }
            is NewCardUiIntent.DifficultySelected -> mutableState.update {
                it.copy(
                    difficulty = intent.difficulty,
                    error = null,
                    learningNotePreview = null,
                    previewResult = null,
                )
            }
            is NewCardUiIntent.TypeViewSelected -> mutableState.update {
                it.copy(
                    typeView = intent.typeView,
                    learningNotePreview = null,
                    previewResult = null
                )
            }
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        mutableState.update { it.copy(isLoading = true, error = null, learningNotePreview = null) }
        runCatching {
            when (current.typeView) {
                TypeView.WordOrPhase -> generateLearningNotePreviewUseCase(
                    input = current.toGenerationInput()
                )
                TypeView.WithCategories -> generateFlashcardPreviewUseCase(
                    word = current.word,
                    categories = current.category,
                    difficulty = current.difficulty,
                    typeView = current.typeView,
                )
            }
        }.onSuccess { preview ->
            when (preview) {
                is GeneratedLearningNote -> mutableState.update {
                    it.copy(
                        learningNotePreview = preview,
                        previewResult = preview.toLegacyFlashcardGenerated(),
                        isLoading = false,
                    )
                }
                is FlashcardGenerated -> mutableState.update {
                    it.copy(previewResult = preview, isLoading = false)
                }
                else -> mutableState.update {
                    it.copy(error = "Tipo de preview no soportado.", isLoading = false)
                }
            }
        }.onFailure { e ->
            mutableState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        val deckId = current.deckSelected?.id ?: return@launch
        val preview = current.previewResult ?: return@launch
        mutableState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            createFlashcardUseCase(
                deckId = deckId,
                flashcard = preview,
            )
        }.onSuccess {
            mutableState.update {
                it.copy(
                    word = "",
                    intendedMeaningEs = "",
                    contextSentence = "",
                    learningNotePreview = null,
                    previewResult = null,
                    isLoading = false,
                    error = null,
                )
            }
            mutableEffect.send(NewCardUiEffect.ShowMessage("Tarjeta creada"))
        }.onFailure { e ->
            mutableState.update { it.copy(error = e.message, isLoading = false) }
            mutableEffect.send(NewCardUiEffect.ShowMessage(e.message ?: "No se pudo guardar la tarjeta"))
        }
    }

    private fun NewCardUiState.toGenerationInput(): FlashcardGenerationInput {
        return FlashcardGenerationInput(
            inputType = word.inferInputType(),
            userText = word,
            intendedMeaningEs = intendedMeaningEs,
            contextSentence = contextSentence,
            learningGoal = LearningGoal.Both,
            levelBand = difficulty.toLevelBand(),
            register = RegisterPreference.Neutral,
            domain = LearningDomain.DailyLife,
        )
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

    private fun GeneratedLearningNote.toLegacyFlashcardGenerated(): FlashcardGenerated {
        val examples = buildList {
            if (exampleSentence.isNotBlank()) {
                add(
                    Example(
                        exampleId = "generated-example",
                        text = exampleSentence,
                        translation = exampleTranslation,
                        type = "main",
                    )
                )
            }
        }
        val notes = listOfNotNull(
            usagePattern.takeIf { it.isNotBlank() },
            commonMistake.takeIf { it.isNotBlank() },
        ).joinToString(separator = " | ")

        val tags = buildList {
            add(levelBand.name)
            add(domain.name)
            addAll(collocations.take(2))
        }

        return FlashcardGenerated(
            word = expression,
            translation = intendedMeaningEs,
            phonetics = ipa,
            meaning = simpleDefinitionEn,
            language = "en",
            examples = examples,
            partOfSpeech = partOfSpeech.name,
            type = noteType.name,
            notes = notes,
            tags = tags,
        )
    }
}
