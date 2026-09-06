package com.emm.hello.newfeatures.capture

import androidx.lifecycle.viewModelScope
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.authoring.RetryFailedEnrichmentsUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.domain.library.LibraryRepository
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val captureFlashcard: CaptureFlashcardUseCase,
    private val retryFailedEnrichments: RetryFailedEnrichmentsUseCase,
    private val enrichmentRepository: FlashcardEnrichmentRepository,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
    getDecksUseCase: GetDecksUseCase,
    libraryRepository: LibraryRepository,
    connectivityRepository: ConnectivityRepository,
) : MviViewModel<CaptureUiState, CaptureUiIntent, CaptureUiEffect>(
    initialState = CaptureUiState(),
) {

    init {
        getDecksUseCase()
            .onEach { decks -> setState { copy(targetDeck = resolveTargetDeck(decks)) } }
            .launchIn(viewModelScope)

        enrichmentRepository.observeBacklog()
            .onEach { backlog -> setState { copy(pending = backlog.pending, failed = backlog.failed) } }
            .launchIn(viewModelScope)

        libraryRepository.observeLibrary()
            .onEach { cards -> setState { copy(recentCaptures = recentCaptures.refreshedFrom(cards)) } }
            .launchIn(viewModelScope)

        connectivityRepository.observeOnline()
            .onEach { isOnline -> setState { copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CaptureUiIntent) {
        when (intent) {
            is CaptureUiIntent.WordChanged -> setState { copy(word = intent.word) }
            CaptureUiIntent.Submit -> handleSubmit()
            CaptureUiIntent.RetryFailed -> handleRetryFailed()
        }
    }

    private fun resolveTargetDeck(decks: List<Deck>): Deck? {
        val defaultDeckId: DeckId? = defaultDeckSelectionRepository.getDefaultDeckId()
        return decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
    }

    private fun handleSubmit() = viewModelScope.launch {
        val current: CaptureUiState = currentState
        val deck: Deck = current.targetDeck ?: return@launch
        if (!current.canSubmit) return@launch

        setState { copy(isSaving = true) }
        try {
            val flashcardId: FlashcardId = captureFlashcard(deckId = deck.id, word = current.word)
            val captured = RecentCapture(
                flashcardId = flashcardId,
                word = current.word.trim(),
                status = EnrichmentStatus.PENDING,
            )
            setState { copy(word = "", isSaving = false, recentCaptures = listOf(captured) + recentCaptures) }
            sendEffect(CaptureUiEffect.EnqueueEnrichment(listOf(flashcardId.value)))
            sendEffect(CaptureUiEffect.ShowMessage(R.string.capture_saved_message))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (validation: DomainValidationException) {
            setState { copy(isSaving = false) }
            sendEffect(CaptureUiEffect.ShowMessage(validation.messageRes()))
        } catch (error: Throwable) {
            logError(TAG, "handleSubmit:error ${error.message}", error)
            setState { copy(isSaving = false) }
            sendEffect(CaptureUiEffect.ShowMessage(R.string.capture_error_generic))
        }
    }

    private fun handleRetryFailed() = viewModelScope.launch {
        try {
            val retried: List<FlashcardId> = retryFailedEnrichments()
            if (retried.isEmpty()) return@launch
            sendEffect(CaptureUiEffect.EnqueueEnrichment(retried.map(FlashcardId::value)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "handleRetryFailed:error ${error.message}", error)
            sendEffect(CaptureUiEffect.ShowMessage(R.string.capture_error_retry))
        }
    }
}

private const val TAG = "CaptureViewModel"

private fun List<RecentCapture>.refreshedFrom(cards: List<LibraryFlashcard>): List<RecentCapture> {
    val statusById: Map<FlashcardId, EnrichmentStatus> = cards.associate { it.id to it.enrichmentStatus }
    return map { capture -> statusById[capture.flashcardId]?.let { capture.copy(status = it) } ?: capture }
}

private fun DomainValidationException.messageRes(): Int {
    val codes: List<IssueCode> = issues.map { it.code }
    return when {
        codes.contains(IssueCode.DuplicateWordInDeck) -> R.string.capture_error_duplicate
        codes.contains(IssueCode.EmptyUserText) -> R.string.capture_error_empty
        else -> R.string.capture_error_generic
    }
}
