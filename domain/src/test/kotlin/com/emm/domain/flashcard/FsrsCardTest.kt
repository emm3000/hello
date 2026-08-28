package com.emm.domain.flashcard

import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FsrsCardTest {

    private val now = Instant.parse("2026-06-12T12:00:00Z")
    private val clock = Clock { now }
    private val flashcardId = "card-1".toFlashcardId()

    @Test
    fun `FsrsState enum has exactly four values`() {
        val expected = listOf(FsrsState.NEW, FsrsState.LEARNING, FsrsState.REVIEW, FsrsState.RELEARNING)
        assertEquals(4, FsrsState.entries.size)
        assertTrue(FsrsState.entries.containsAll(expected))
    }

    @Test
    fun `new card has NEW state and zero stability and zero difficulty`() {
        val card = FsrsCard.new(flashcardId, clock)

        assertEquals(FsrsState.NEW, card.state)
        assertEquals(0.0, card.stability, 0.0)
        assertEquals(0.0, card.difficulty, 0.0)
    }

    @Test
    fun `new card timestamps both set to clock now`() {
        val card = FsrsCard.new(flashcardId, clock)

        val nowMs = now.toEpochMilli()
        assertEquals(nowMs, card.lastReviewedAt)
        assertEquals(nowMs, card.nextReviewAt)
    }

    @Test
    fun `new card has zero reps, zero lapses, zero interval`() {
        val card = FsrsCard.new(flashcardId, clock)

        assertEquals(0L, card.reps)
        assertEquals(0L, card.lapses)
        assertEquals(0L, card.interval)
    }

    @Test
    fun `new card carries the given flashcardId`() {
        val card = FsrsCard.new(flashcardId, clock)

        assertEquals(flashcardId, card.flashcardId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative stability`() {
        validCard().copy(stability = -0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects difficulty below 1 for non-NEW card`() {
        // Non-NEW cards must have difficulty in [1.0, 10.0]; 0.0 is only valid for NEW state.
        validCard().copy(state = FsrsState.LEARNING, stability = 1.0, difficulty = 0.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects difficulty above 10 for non-NEW card`() {
        validCard().copy(state = FsrsState.LEARNING, stability = 1.0, difficulty = 10.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects nextReviewAt before lastReviewedAt`() {
        validCard().copy(lastReviewedAt = 1000L, nextReviewAt = 999L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative reps`() {
        validCard().copy(reps = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative lapses`() {
        validCard().copy(lapses = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative interval`() {
        validCard().copy(interval = -1L)
    }

    @Test
    fun `constructor accepts difficulty exactly 1`() {
        val card = validCard().copy(difficulty = 1.0)
        assertEquals(1.0, card.difficulty, 0.0)
    }

    @Test
    fun `constructor accepts difficulty exactly 10`() {
        val card = validCard().copy(difficulty = 10.0)
        assertEquals(10.0, card.difficulty, 0.0)
    }

    @Test
    fun `constructor accepts zero stability for NEW card`() {
        val card = validCard().copy(stability = 0.0)
        assertEquals(0.0, card.stability, 0.0)
    }

    @Test
    fun `invariants hold when reps is 1 after first rating simulation`() {
        val card = validCard().copy(
            state = FsrsState.LEARNING,
            stability = 2.5,
            difficulty = 5.0,
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            reps = 1L,
            lapses = 0L,
            interval = 1L,
        )
        assertTrue(card.stability >= 0.0)
        assertTrue(card.difficulty in 1.0..10.0)
        assertTrue(card.nextReviewAt >= card.lastReviewedAt)
        assertEquals(1L, card.reps)
        assertEquals(0L, card.lapses)
    }

    private fun validCard() = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.NEW,
        stability = 0.0,
        difficulty = 0.0,
        lastReviewedAt = now.toEpochMilli(),
        nextReviewAt = now.toEpochMilli(),
        interval = 0L,
        reps = 0L,
        lapses = 0L,
    )
}
