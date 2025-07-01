package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FlashcardFetcher(
    private val repository: FlashcardRepository,
    private val flashcardReviewRepository: FlashcardReviewRepository,
) {

    fun fetchAll(deckId: String): Flow<List<Flashcard>> = combine(
        flow = repository.fetchByDeckId(deckId),
        flow2 = flashcardReviewRepository.all(),
        transform = ::mapReviewToFlashcard,
    )

    private fun mapReviewToFlashcard(
        flashcards: List<Flashcard>,
        reviews: List<FlashcardReview>,
    ): List<Flashcard> {
        val reviewsByFlashcardId: Map<String, FlashcardReview> = reviews.associateBy(FlashcardReview::flashcardId)
        return flashcards.map { flashcard -> attachReviewToFlashcard(flashcard, reviewsByFlashcardId) }
    }

    private fun attachReviewToFlashcard(
        flashcard: Flashcard,
        reviewsByFlashcardId: Map<String, FlashcardReview>,
    ): Flashcard = Flashcard(
        id = flashcard.id,
        word = flashcard.word,
        meaning = flashcard.meaning,
        translation = flashcard.translation,
        examples = flashcard.examples,
        phonetic = flashcard.phonetic,
        review = reviewsByFlashcardId[flashcard.id] ?: FlashcardReview.Empty,
    )
}