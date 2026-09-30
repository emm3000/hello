package com.emm.hello.newfeatures.capture

import androidx.lifecycle.viewModelScope
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.authoring.CreateManualFlashcardUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.CaptureDeckChoice
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.ResolveCaptureDeckUseCase
import com.emm.domain.flashcard.EnrichmentStatus
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
    private val createManualFlashcard: CreateManualFlashcardUseCase,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
    private val resolveCaptureDeck: ResolveCaptureDeckUseCase,
    getDecksUseCase: GetDecksUseCase,
    libraryRepository: LibraryRepository,
    connectivityRepository: ConnectivityRepository,
) : MviViewModel<CaptureUiState, CaptureUiIntent, CaptureUiEffect>(
    initialState = CaptureUiState(),
) {

    init {
        getDecksUseCase()
            .onEach { decks -> showCaptureDecks(resolveCaptureDeck(decks)) }
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
            is CaptureUiIntent.WordChanged -> setState { copy(word = intent.word, wordErrorRes = null) }
            is CaptureUiIntent.ManualModeSelected -> selectManualMode(intent.isManual)
            is CaptureUiIntent.TranslationChanged ->
                setState { copy(translation = intent.translation, translationErrorRes = null) }
            is CaptureUiIntent.MeaningChanged -> setState { copy(meaning = intent.meaning) }
            CaptureUiIntent.Submit -> handleSubmit()
            CaptureUiIntent.DeckPickerOpened -> setState { copy(isDeckPickerOpen = true) }
            CaptureUiIntent.DeckPickerDismissed -> setState { copy(isDeckPickerOpen = false) }
            is CaptureUiIntent.DeckSelected -> selectDeck(intent.deckId)
            is CaptureUiIntent.RecentCaptureClicked -> openRecentCapture(intent.flashcardId)
        }
    }

    private fun openRecentCapture(flashcardId: FlashcardId) {
        val capture: RecentCapture = currentState.recentCaptures.find { it.flashcardId == flashcardId } ?: return
        sendEffect(CaptureUiEffect.OpenCard(cardId = capture.flashcardId.value, deckId = capture.deckId.value))
    }

    private fun showCaptureDecks(choice: CaptureDeckChoice) {
        setState { copy(decks = choice.candidates, targetDeck = choice.target) }
    }

    private fun selectDeck(deckId: DeckId) {
        val selected: Deck? = currentState.decks.find { it.id == deckId }
        if (selected == null) {
            setState { copy(isDeckPickerOpen = false) }
            return
        }
        defaultDeckSelectionRepository.setDefaultDeckId(deckId)
        setState { copy(targetDeck = selected, isDeckPickerOpen = false) }
    }

    private fun selectManualMode(isManual: Boolean) = setState {
        if (isManual) {
            copy(isManual = true)
        } else {
            copy(isManual = false, translation = "", meaning = "", translationErrorRes = null)
        }
    }

    private fun handleSubmit() {
        val current: CaptureUiState = currentState
        val deck: Deck = current.targetDeck ?: return
        if (current.isSaving || current.word.isBlank()) return
        if (current.isManual && current.translation.isBlank()) {
            setState { copy(translationErrorRes = R.string.capture_error_translation_required) }
            return
        }

        setState { copy(isSaving = true) }
        viewModelScope.launch { save(deck = deck, current = current) }
    }

    private suspend fun save(deck: Deck, current: CaptureUiState) {
        try {
            if (current.isManual) {
                saveWrittenCard(deck = deck, current = current)
            } else {
                saveForEnrichment(deck = deck, current = current)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (validation: DomainValidationException) {
            showValidationFailure(validation)
        } catch (error: Throwable) {
            logError(TAG, "handleSubmit:error ${error.message}", error)
            setState { copy(isSaving = false) }
            sendEffect(CaptureUiEffect.ShowMessage(R.string.capture_error_generic))
        }
    }

    private fun showValidationFailure(validation: DomainValidationException) {
        val codes: List<IssueCode> = validation.issues.map { it.code }
        when {
            IssueCode.DuplicateWordInDeck in codes ->
                setState { copy(isSaving = false, wordErrorRes = R.string.capture_error_duplicate) }
            IssueCode.EmptyTranslation in codes ->
                setState { copy(isSaving = false, translationErrorRes = R.string.capture_error_translation_required) }
            IssueCode.EmptyUserText in codes -> setState { copy(isSaving = false) }
            else -> {
                setState { copy(isSaving = false) }
                sendEffect(CaptureUiEffect.ShowMessage(R.string.capture_error_generic))
            }
        }
    }

    private suspend fun saveForEnrichment(deck: Deck, current: CaptureUiState) {
        val flashcardId: FlashcardId = captureFlashcard(deckId = deck.id, word = current.word)
        val captured = RecentCapture(
            flashcardId = flashcardId,
            deckId = deck.id,
            word = current.word.trim(),
            status = EnrichmentStatus.PENDING,
        )
        setState { copy(word = "", isSaving = false, recentCaptures = listOf(captured) + recentCaptures) }
        sendEffect(CaptureUiEffect.EnqueueEnrichment(listOf(flashcardId.value)))
    }

    private suspend fun saveWrittenCard(deck: Deck, current: CaptureUiState) {
        val flashcardId: FlashcardId = createManualFlashcard(
            deckId = deck.id,
            word = current.word,
            translation = current.translation,
            meaning = current.meaning,
        )
        val captured = RecentCapture(
            flashcardId = flashcardId,
            deckId = deck.id,
            word = current.word.trim(),
            status = EnrichmentStatus.ENRICHED,
        )
        setState {
            copy(
                word = "",
                translation = "",
                meaning = "",
                isSaving = false,
                recentCaptures = listOf(captured) + recentCaptures,
            )
        }
    }
}

private const val TAG = "CaptureViewModel"

private fun List<RecentCapture>.refreshedFrom(cards: List<LibraryFlashcard>): List<RecentCapture> {
    val cardsById: Map<FlashcardId, LibraryFlashcard> = cards.associateBy { it.id }
    return map { capture ->
        val card: LibraryFlashcard = cardsById[capture.flashcardId] ?: return@map capture
        capture.copy(deckId = card.deckId, status = card.enrichmentStatus, failure = card.enrichmentFailure)
    }
}
