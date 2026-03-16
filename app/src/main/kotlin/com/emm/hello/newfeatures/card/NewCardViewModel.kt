package com.emm.hello.newfeatures.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.GenerateFlashcardPreviewUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewCardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val generateFlashcardPreviewUseCase: GenerateFlashcardPreviewUseCase,
    private val getDefaultDeckUseCase: GetDefaultDeckUseCase,
    private val setDefaultDeckUseCase: SetDefaultDeckUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewCardUiState())
    val uiState = _state.asStateFlow()

    private val _effect = Channel<NewCardUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        getDecksUseCase()
            .onEach { decks ->
                val defaultDeckId = getDefaultDeckUseCase()
                val selectedDeck = decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
                _state.update {
                    it.copy(
                        decks = decks,
                        deckSelected = selectedDeck,
                        isCheck = defaultDeckId.isNotEmpty() && selectedDeck?.id == defaultDeckId
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: NewCardUiIntent) {
        when (intent) {
            is NewCardUiIntent.DeckSelected -> _state.update {
                it.copy(
                    deckSelected = intent.deck,
                    isCheck = getDefaultDeckUseCase() == intent.deck.id,
                )
            }
            is NewCardUiIntent.WordChanged -> _state.update {
                it.copy(
                    word = intent.word,
                    error = null,
                    previewResult = null
                )
            }
            is NewCardUiIntent.CheckChanged -> {
                val newDeckId = if (intent.checked) _state.value.deckSelected?.id.orEmpty() else ""
                setDefaultDeckUseCase(newDeckId)
                _state.update { it.copy(isCheck = intent.checked) }
            }
            NewCardUiIntent.GenerateClicked -> generateFlashcard()
            NewCardUiIntent.SaveClicked -> saveFlashcard()
            is NewCardUiIntent.CategorySelected -> _state.update {
                it.copy(
                    category = intent.category,
                    error = null,
                    previewResult = null
                )
            }
            is NewCardUiIntent.DifficultySelected -> _state.update {
                it.copy(
                    difficulty = intent.difficulty,
                    error = null,
                    previewResult = null,
                )
            }
            is NewCardUiIntent.TypeViewSelected -> _state.update {
                it.copy(
                    typeView = intent.typeView,
                    previewResult = null
                )
            }
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching {
            val preview = generateFlashcardPreviewUseCase(
                word = current.word,
                categories = current.category,
                difficulty = current.difficulty,
                typeView = current.typeView,
            )
            preview
        }.onSuccess { preview ->
            _state.update { it.copy(previewResult = preview, isLoading = false) }
        }.onFailure { e ->
            _state.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val current = _state.value
        val deckId = current.deckSelected?.id ?: return@launch
        val preview = current.previewResult ?: return@launch
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching {
            createFlashcardUseCase(
                deckId = deckId,
                flashcard = preview,
            )
        }.onSuccess {
            _state.update {
                it.copy(
                    word = "",
                    previewResult = null,
                    isLoading = false,
                    error = null,
                )
            }
            _effect.send(NewCardUiEffect.ShowMessage("Tarjeta creada"))
        }.onFailure { e ->
            _state.update { it.copy(error = e.message, isLoading = false) }
            _effect.send(
                NewCardUiEffect.ShowMessage(
                    e.message ?: "No se pudo guardar la tarjeta"
                )
            )
        }
    }
}
