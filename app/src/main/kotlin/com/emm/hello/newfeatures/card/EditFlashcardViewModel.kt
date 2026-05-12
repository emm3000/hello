package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditFlashcardViewModel(
    private val flashcardId: String,
    private val deckId: String,
    private val flashcardRepository: FlashcardRepository,
    private val updateFlashcardUseCase: UpdateFlashcardUseCase,
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
            is EditFlashcardUiIntent.TranslationChanged -> handleTranslationChanged(intent.translation)
            is EditFlashcardUiIntent.PhoneticChanged -> handlePhoneticChanged(intent.phonetic)
            is EditFlashcardUiIntent.PartOfSpeechChanged -> handlePartOfSpeechChanged(intent.partOfSpeech)
            is EditFlashcardUiIntent.ExampleTextChanged -> handleExampleTextChanged(intent.index, intent.text)
            is EditFlashcardUiIntent.ExampleTranslationChanged -> handleExampleTranslationChanged(
                intent.index,
                intent.translation,
            )
            EditFlashcardUiIntent.AddExample -> handleAddExample()
            is EditFlashcardUiIntent.RemoveExample -> handleRemoveExample(intent.index)
            EditFlashcardUiIntent.Submit -> handleSubmit()
        }
    }

    private fun loadFlashcard() = viewModelScope.launch {
        mutableState.update { it.copy(isLoading = true) }
        runCatching {
            flashcardRepository.fetchById(flashcardId.toFlashcardId())
        }.onSuccess { detail ->
            val card = detail.flashcard
            mutableState.update {
                it.copy(
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
            mutableState.update { it.copy(isLoading = false) }
            mutableEffect.send(
                EditFlashcardUiEffect.ShowMessage(error.message ?: "No se pudo cargar la tarjeta")
            )
        }
    }

    private fun handleWordChanged(word: String) {
        val error = if (word.isBlank()) "La palabra es obligatoria" else null
        mutableState.update { it.copy(word = word, wordError = error) }
    }

    private fun handleMeaningChanged(meaning: String) {
        val error = if (meaning.isBlank()) "El significado es obligatorio" else null
        mutableState.update { it.copy(meaning = meaning, meaningError = error) }
    }

    private fun handleTranslationChanged(translation: String) {
        mutableState.update { it.copy(translation = translation) }
    }

    private fun handlePhoneticChanged(phonetic: String) {
        mutableState.update { it.copy(phonetic = phonetic) }
    }

    private fun handlePartOfSpeechChanged(partOfSpeech: String) {
        mutableState.update { it.copy(partOfSpeech = partOfSpeech) }
    }

    private fun handleExampleTextChanged(index: Int, text: String) {
        val current = mutableState.value.examples
        if (index !in current.indices) return
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(text = text)
        mutableState.update { it.copy(examples = updated) }
    }

    private fun handleExampleTranslationChanged(index: Int, translation: String) {
        val current = mutableState.value.examples
        if (index !in current.indices) return
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(translation = translation)
        mutableState.update { it.copy(examples = updated) }
    }

    private fun handleAddExample() {
        val current = mutableState.value.examples
        val newExample = com.emm.domain.flashcard.Example(
            exampleId = "",
            text = "",
            translation = "",
            type = "",
        )
        mutableState.update { it.copy(examples = current + newExample) }
    }

    private fun handleRemoveExample(index: Int) {
        val current = mutableState.value.examples
        if (index !in current.indices) return
        mutableState.update { it.copy(examples = current.toMutableList().apply { removeAt(index) }) }
    }

    private fun handleSubmit() = viewModelScope.launch {
        val current = mutableState.value
        if (!current.isValid || current.isSubmitting) return@launch

        mutableState.update { it.copy(isSubmitting = true) }
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
            mutableEffect.send(EditFlashcardUiEffect.NavigateBack)
        }.onFailure { error ->
            logError(TAG, "handleSubmit:error ${error.message}", error)
            mutableState.update { it.copy(isSubmitting = false) }
            mutableEffect.send(
                EditFlashcardUiEffect.ShowMessage(error.message ?: "No se pudo actualizar la tarjeta")
            )
        }
    }
}

private const val TAG = "EditFlashcardViewModel"