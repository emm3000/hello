package com.emm.hello.newfeatures.deck

import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DeckDetailViewModelTest {

    @Test
    fun `merge deck cards by id preserves all deck cards when session list is shorter`() {
        val baseReview = FlashcardReview.Empty.copy(nextReviewAt = 10L)
        val updatedReview = FlashcardReview.Empty.copy(nextReviewAt = 99L)
        val cardA = flashcard(id = "a", review = baseReview)
        val cardB = flashcard(id = "b", review = baseReview)

        val merged = mergeDeckCardsById(
            deckCards = listOf(cardA, cardB),
            sessionCards = listOf(flashcard(id = "a", review = updatedReview)),
        )

        assertEquals(2, merged.size)
        assertEquals(99L, merged[0].review.nextReviewAt)
        assertSame(cardB.review, merged[1].review)
    }

    private fun flashcard(id: String, review: FlashcardReview): Flashcard = Flashcard(
        id = id,
        word = id,
        meaning = "",
        translation = "",
        examples = emptyList<Example>(),
        phonetic = "",
        review = review,
    )
}
