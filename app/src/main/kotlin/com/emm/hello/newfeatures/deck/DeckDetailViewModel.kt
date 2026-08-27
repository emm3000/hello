package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

class DeckDetailViewModel(
    private val deckId: String,
    getDeckDetailUseCase: GetDeckDetailUseCase,
    private val observeFlashcardsWithReviewUseCase: ObserveFlashcardsWithReviewUseCase,
    private val softDeleteDeckUseCase: SoftDeleteDeckUseCase,
    private val restoreFlashcardUseCase: RestoreFlashcardUseCase,
    private val undoEventHolder: UndoEventHolder,
) : MviViewModel<DeckDetailUiState, DeckDetailUiIntent, DeckDetailUiEffect>(
    initialState = DeckDetailUiState(),
) {

    init {
        // Collect card-deleted undo events so this screen can offer undo for flashcard deletions
        // that happened in CardDetail (which navigates back here).
        undoEventHolder.events
            .filterIsInstance<UndoEvent.CardDeleted>()
            .onEach { event ->
                sendEffect(
                    DeckDetailUiEffect.ShowUndoCardDeleted(
                        flashcardId = event.flashcardId,
                        deletedAt = event.deletedAt,
                    )
                )
            }
            .launchIn(viewModelScope)

        combine(
            flow = getDeckDetailUseCase(deckId),
            flow2 = fetchSessionCards(),
            transform = { deck: com.emm.domain.deck.Deck?, (sessionCards, hasSessionEnabled) ->
                if (deck == null) {
                    null
                } else {
                    val mergedCards = mergeDeckCardsById(deck.cards, sessionCards)
                    deck.copy(cards = mergedCards) to hasSessionEnabled
                }
            }
        )
            .filterNotNull()
            .onEach { (deck, hasSessionEnabled) ->
                setState { copy(deck = deck, hasSessionEnabled = hasSessionEnabled) }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchSessionCards(): Flow<Pair<List<Flashcard>, Boolean>> =
        observeFlashcardsWithReviewUseCase(deckId).map { studyFlashcards ->
            val flashcards: List<Flashcard> = studyFlashcards.map { it.toFlashcard() }
            val nowMillis: Long = Instant.now().toEpochMilli()
            val hasSessionEnabled: Boolean = studyFlashcards.any {
                it.enrichmentStatus == EnrichmentStatus.ENRICHED && it.review.nextReviewAt <= nowMillis
            }
            flashcards to hasSessionEnabled
        }

    override fun onIntent(intent: DeckDetailUiIntent) {
        when (intent) {
            is DeckDetailUiIntent.SearchCardsChanged -> setState { copy(searchQuery = intent.query) }
            DeckDetailUiIntent.EditDeck -> sendEffect(DeckDetailUiEffect.NavigateToEditDeck(deckId))
            DeckDetailUiIntent.DeleteDeck -> setState { copy(isDeleteConfirmationVisible = true) }
            DeckDetailUiIntent.ConfirmDeleteDeck -> deleteDeck()
            DeckDetailUiIntent.DismissDeleteDeck -> setState { copy(isDeleteConfirmationVisible = false) }
            is DeckDetailUiIntent.UndoDeleteCard -> undoDeleteCard(intent.flashcardId, intent.deletedAt)
        }
    }

    private fun deleteDeck() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        try {
            val deletedAt = softDeleteDeckUseCase(deckId.toDeckId())
            undoEventHolder.tryEmit(
                UndoEvent.DeckDeleted(
                    deckId = deckId,
                    deletedAt = deletedAt,
                    deckName = currentState.deck.name,
                )
            )
            sendEffect(DeckDetailUiEffect.DeckDeleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "deleteDeck:error ${e.message}", e)
            sendEffect(DeckDetailUiEffect.ShowMessage(R.string.error_delete_deck))
        }
    }

    private fun undoDeleteCard(flashcardId: String, deletedAt: Long) = viewModelScope.launch {
        try {
            restoreFlashcardUseCase(flashcardId.toFlashcardId(), deletedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "undoDeleteCard:error ${e.message}", e)
            sendEffect(DeckDetailUiEffect.ShowMessage(R.string.error_restore_card))
        }
        // On success, the reactive flashcard flow (ObserveFlashcardsWithReviewUseCase) refreshes automatically.
    }
}

internal fun matchesSearchQuery(card: Flashcard, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return card.word.contains(trimmed, ignoreCase = true) ||
        card.translation.contains(trimmed, ignoreCase = true) ||
        card.meaning.contains(trimmed, ignoreCase = true)
}

internal fun mergeDeckCardsById(
    deckCards: List<Flashcard>,
    sessionCards: List<Flashcard>,
): List<Flashcard> {
    val reviewsByCardId = sessionCards.associateBy(Flashcard::id)
    return deckCards.map { deckCard ->
        val sessionCard = reviewsByCardId[deckCard.id]
        if (sessionCard == null) deckCard else deckCard.copy(review = sessionCard.review)
    }
}

private fun StudyFlashcard.toFlashcard(): Flashcard = Flashcard(
    id = flashcardId,
    word = word,
    meaning = meaning,
    translation = translation,
    examples = emptyList(),
    phonetic = phonetic,
    review = review,
    usagePattern = usagePattern,
    whyUseful = whyUseful,
    sourceContext = sourceContext,
    irregularForms = irregularForms,
)

private const val TAG = "DeckDetailViewModel"
