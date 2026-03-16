package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.GenerateFlashcardPreviewUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val generateFlashcardPreviewUseCase: GenerateFlashcardPreviewUseCase,
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
                    previewResult = null
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
                    previewResult = null
                )
            }
            is NewCardUiIntent.DifficultySelected -> mutableState.update {
                it.copy(
                    difficulty = intent.difficulty,
                    error = null,
                    previewResult = null,
                )
            }
            is NewCardUiIntent.TypeViewSelected -> mutableState.update {
                it.copy(
                    typeView = intent.typeView,
                    previewResult = null
                )
            }
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        val current = mutableState.value
        mutableState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            generateFlashcardPreviewUseCase(
                word = current.word,
                categories = current.category,
                difficulty = current.difficulty,
                typeView = current.typeView,
            )
        }.onSuccess { preview ->
            mutableState.update { it.copy(previewResult = preview, isLoading = false) }
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
}
