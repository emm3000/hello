package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant

class DeckDetailViewModel(
    private val deckId: String,
    getDeckDetailUseCase: GetDeckDetailUseCase,
    private val observeFlashcardsWithReviewUseCase: ObserveFlashcardsWithReviewUseCase,
    private val softDeleteDeckUseCase: SoftDeleteDeckUseCase,
) : MviViewModel<DeckDetailUiState, DeckDetailUiIntent, DeckDetailUiEffect>(
    initialState = DeckDetailUiState(),
) {

    init {
        combine(
            flow = getDeckDetailUseCase(deckId),
            flow2 = fetchSessionCards(),
            transform = { deck, (sessionCards, hasSessionEnabled) ->
                val mergedCards = mergeDeckCardsById(deck.cards, sessionCards)
                deck.copy(cards = mergedCards) to hasSessionEnabled
            }
        )
            .onEach { (deck, hasSessionEnabled) ->
                setState { copy(deck = deck, hasSessionEnabled = hasSessionEnabled) }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchSessionCards(): Flow<Pair<List<Flashcard>, Boolean>> =
        observeFlashcardsWithReviewUseCase(deckId).map { studyFlashcards ->
            val flashcards = studyFlashcards.map { it.toFlashcard() }
            val hasSessionEnabled = studyFlashcards.any { it.review.nextReviewAt <= Instant.now().toEpochMilli() }
            flashcards to hasSessionEnabled
        }

    override fun onIntent(intent: DeckDetailUiIntent) {
        when (intent) {
            is DeckDetailUiIntent.SearchCardsChanged -> setState { copy(searchQuery = intent.query) }
            DeckDetailUiIntent.EditDeck -> sendEffect(DeckDetailUiEffect.NavigateToEditDeck(deckId))
            DeckDetailUiIntent.DeleteDeck -> setState { copy(isDeleteConfirmationVisible = true) }
            DeckDetailUiIntent.ConfirmDeleteDeck -> deleteDeck()
            DeckDetailUiIntent.DismissDeleteDeck -> setState { copy(isDeleteConfirmationVisible = false) }
        }
    }

    private fun deleteDeck() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        runCatching {
            softDeleteDeckUseCase(deckId.toDeckId())
        }.onSuccess {
            sendEffect(DeckDetailUiEffect.DeckDeleted)
        }.onFailure { error ->
            logError(TAG, "deleteDeck:error ${error.message}", error)
            sendEffect(DeckDetailUiEffect.ShowMessage("No se pudo eliminar el mazo"))
        }
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
