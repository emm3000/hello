package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class EditFlashcardViewModel(
    private val flashcardId: String,
    private val flashcardRepository: FlashcardRepository,
    private val updateFlashcardUseCase: UpdateFlashcardUseCase,
    private val softDeleteFlashcardUseCase: SoftDeleteFlashcardUseCase,
) : MviViewModel<EditFlashcardUiState, EditFlashcardUiIntent, EditFlashcardUiEffect>(
    initialState = EditFlashcardUiState(flashcardId = flashcardId),
) {

    private var loadedCard: Flashcard? = null

    init {
        loadFlashcard()
    }

    override fun onIntent(intent: EditFlashcardUiIntent) {
        when (intent) {
            is EditFlashcardUiIntent.WordChanged -> handleWordChanged(intent.word)
            is EditFlashcardUiIntent.TranslationChanged -> setState { copy(translation = intent.translation) }
            is EditFlashcardUiIntent.ExampleTextChanged -> setState { copy(exampleText = intent.text) }
            is EditFlashcardUiIntent.ExampleTranslationChanged -> {
                setState { copy(exampleTranslation = intent.translation) }
            }
            is EditFlashcardUiIntent.PartOfSpeechChanged -> setState { copy(partOfSpeech = intent.partOfSpeech) }
            is EditFlashcardUiIntent.PhoneticChanged -> setState { copy(phonetic = intent.phonetic) }
            EditFlashcardUiIntent.CloseClicked -> sendEffect(EditFlashcardUiEffect.NavigateBack)
            EditFlashcardUiIntent.Submit -> handleSubmit()
            EditFlashcardUiIntent.DeleteFlashcard -> setState { copy(isDeleteConfirmationVisible = true) }
            EditFlashcardUiIntent.ConfirmDeleteFlashcard -> handleDelete()
            EditFlashcardUiIntent.DismissDeleteFlashcard -> setState { copy(isDeleteConfirmationVisible = false) }
        }
    }

    private fun loadFlashcard() = viewModelScope.launch {
        setState { copy(isLoading = true) }
        try {
            val card: Flashcard = flashcardRepository.fetchById(flashcardId.toFlashcardId()).flashcard
            loadedCard = card
            val firstExample: Example? = card.examples.firstOrNull()
            setState {
                copy(
                    word = card.word,
                    translation = card.translation,
                    exampleText = firstExample?.text.orEmpty(),
                    exampleTranslation = firstExample?.translation.orEmpty(),
                    partOfSpeech = card.partOfSpeech,
                    phonetic = card.phonetic,
                    isLoading = false,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "loadFlashcard:error ${e.message}", e)
            setState { copy(isLoading = false) }
            sendEffect(EditFlashcardUiEffect.ShowMessage(R.string.error_load_card))
        }
    }

    private fun handleWordChanged(word: String) {
        val error: Int? = if (word.isBlank()) R.string.validation_word_required else null
        setState { copy(word = word, wordError = error) }
    }

    private fun handleSubmit() = viewModelScope.launch {
        val current: EditFlashcardUiState = currentState
        if (!current.isValid || current.isSubmitting) return@launch

        setState { copy(isSubmitting = true) }
        try {
            updateFlashcardUseCase(
                UpdateFlashcardInput(
                    flashcardId = flashcardId.toFlashcardId(),
                    word = current.word,
                    meaning = loadedCard?.meaning.orEmpty(),
                    translation = current.translation,
                    phonetic = current.phonetic,
                    partOfSpeech = current.partOfSpeech,
                    examples = mergedExamples(current),
                )
            )
            sendEffect(EditFlashcardUiEffect.ShowMessage(R.string.card_updated_message))
            sendEffect(EditFlashcardUiEffect.NavigateBack)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "handleSubmit:error ${e.message}", e)
            setState { copy(isSubmitting = false) }
            sendEffect(EditFlashcardUiEffect.ShowMessage(R.string.error_save_card))
        }
    }

    private fun mergedExamples(current: EditFlashcardUiState): List<Example> {
        val loaded: List<Example> = loadedCard?.examples.orEmpty()
        val hasContent: Boolean = current.exampleText.isNotBlank() || current.exampleTranslation.isNotBlank()
        val first: Example? = loaded.firstOrNull()
        val head: List<Example> = when {
            !hasContent -> emptyList()
            first != null -> listOf(first.copy(text = current.exampleText, translation = current.exampleTranslation))
            else -> listOf(
                Example(
                    exampleId = "",
                    text = current.exampleText,
                    translation = current.exampleTranslation,
                    type = "",
                ),
            )
        }
        return head + loaded.drop(1)
    }

    private fun handleDelete() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        try {
            softDeleteFlashcardUseCase(flashcardId.toFlashcardId())
            sendEffect(EditFlashcardUiEffect.FlashcardDeleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "handleDelete:error ${e.message}", e)
            sendEffect(EditFlashcardUiEffect.ShowMessage(R.string.error_delete_card))
        }
    }
}

private const val TAG = "EditFlashcardViewModel"
