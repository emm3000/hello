package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.launch

class EditFlashcardViewModel(
    private val flashcardId: String,
    private val deckId: String,
    private val flashcardRepository: FlashcardRepository,
    private val updateFlashcardUseCase: UpdateFlashcardUseCase,
    private val softDeleteFlashcardUseCase: SoftDeleteFlashcardUseCase,
) : MviViewModel<EditFlashcardUiState, EditFlashcardUiIntent, EditFlashcardUiEffect>(
    initialState = EditFlashcardUiState(),
) {

    init {
        loadFlashcard()
    }

    override fun onIntent(intent: EditFlashcardUiIntent) {
        when (intent) {
            is EditFlashcardUiIntent.WordChanged -> handleWordChanged(intent.word)
            is EditFlashcardUiIntent.MeaningChanged -> handleMeaningChanged(intent.meaning)
            is EditFlashcardUiIntent.TranslationChanged -> setState { copy(translation = intent.translation) }
            is EditFlashcardUiIntent.PhoneticChanged -> setState { copy(phonetic = intent.phonetic) }
            is EditFlashcardUiIntent.PartOfSpeechChanged -> setState { copy(partOfSpeech = intent.partOfSpeech) }
            is EditFlashcardUiIntent.ExampleTextChanged -> handleExampleTextChanged(intent.index, intent.text)
            is EditFlashcardUiIntent.ExampleTranslationChanged -> handleExampleTranslationChanged(
                intent.index,
                intent.translation,
            )
            EditFlashcardUiIntent.AddExample -> handleAddExample()
            is EditFlashcardUiIntent.RemoveExample -> handleRemoveExample(intent.index)
            EditFlashcardUiIntent.Submit -> handleSubmit()
            EditFlashcardUiIntent.DeleteFlashcard -> setState { copy(isDeleteConfirmationVisible = true) }
            EditFlashcardUiIntent.ConfirmDeleteFlashcard -> handleDelete()
            EditFlashcardUiIntent.DismissDeleteFlashcard -> setState { copy(isDeleteConfirmationVisible = false) }
        }
    }

    private fun loadFlashcard() = viewModelScope.launch {
        setState { copy(isLoading = true) }
        runCatching {
            flashcardRepository.fetchById(flashcardId.toFlashcardId())
        }.onSuccess { detail ->
            val card = detail.flashcard
            setState {
                copy(
                    word = card.word,
                    meaning = card.meaning,
                    translation = card.translation,
                    phonetic = card.phonetic,
                    partOfSpeech = card.partOfSpeech,
                    examples = card.examples,
                    isLoading = false,
                )
            }
        }.onFailure { error ->
            logError(TAG, "loadFlashcard:error ${error.message}", error)
            setState { copy(isLoading = false) }
            sendEffect(EditFlashcardUiEffect.ShowMessage(error.message ?: "No se pudo cargar la tarjeta"))
        }
    }

    private fun handleWordChanged(word: String) {
        val error = if (word.isBlank()) "La palabra es obligatoria" else null
        setState { copy(word = word, wordError = error) }
    }

    private fun handleMeaningChanged(meaning: String) {
        val error = if (meaning.isBlank()) "El significado es obligatorio" else null
        setState { copy(meaning = meaning, meaningError = error) }
    }

    private fun handleExampleTextChanged(index: Int, text: String) {
        val current = currentState.examples
        if (index !in current.indices) return
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(text = text)
        setState { copy(examples = updated) }
    }

    private fun handleExampleTranslationChanged(index: Int, translation: String) {
        val current = currentState.examples
        if (index !in current.indices) return
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(translation = translation)
        setState { copy(examples = updated) }
    }

    private fun handleAddExample() {
        val newExample = Example(
            exampleId = "",
            text = "",
            translation = "",
            type = "",
        )
        setState { copy(examples = examples + newExample) }
    }

    private fun handleRemoveExample(index: Int) {
        val current = currentState.examples
        if (index !in current.indices) return
        setState { copy(examples = current.toMutableList().apply { removeAt(index) }) }
    }

    private fun handleDelete() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        runCatching {
            softDeleteFlashcardUseCase(flashcardId.toFlashcardId())
        }.onSuccess {
            sendEffect(EditFlashcardUiEffect.FlashcardDeleted)
        }.onFailure { error ->
            logError(TAG, "handleDelete:error ${error.message}", error)
            sendEffect(EditFlashcardUiEffect.ShowMessage(error.message ?: "No se pudo eliminar la tarjeta"))
        }
    }

    private fun handleSubmit() = viewModelScope.launch {
        val current = currentState
        if (!current.isValid || current.isSubmitting) return@launch

        setState { copy(isSubmitting = true) }
        runCatching {
            updateFlashcardUseCase(
                UpdateFlashcardInput(
                    flashcardId = flashcardId.toFlashcardId(),
                    deckId = deckId.toDeckId(),
                    word = current.word,
                    meaning = current.meaning,
                    translation = current.translation,
                    phonetic = current.phonetic,
                    partOfSpeech = current.partOfSpeech,
                    examples = current.examples,
                )
            )
        }.onSuccess {
            sendEffect(EditFlashcardUiEffect.NavigateBack)
        }.onFailure { error ->
            logError(TAG, "handleSubmit:error ${error.message}", error)
            setState { copy(isSubmitting = false) }
            sendEffect(EditFlashcardUiEffect.ShowMessage(error.message ?: "No se pudo actualizar la tarjeta"))
        }
    }
}

private const val TAG = "EditFlashcardViewModel"
