package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.ids.DeckId
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Regression for the deck-detail crash: cards whose stored nextReviewAt is already in the
 * past must map without violating the FlashcardReview nextReviewAt >= lastReviewedAt invariant.
 */
class FlashcardWithReviewMappingTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultFlashcardRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultFlashcardRepository(
            db = db,
            geminiService = mockk<GeminiService>(),
            json = Json,
        )
    }

    @Test
    fun `reviewed card with past nextReviewAt maps without crashing`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)
        // Both timestamps deep in the past relative to the real clock.
        insertProjection(cardId, lastReviewedAt = 1_000L, nextReviewAt = 2_000L)

        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        assertEquals(1, cards.size)
        assertEquals(1_000L, cards.first().review.lastReviewedAt)
        assertEquals(2_000L, cards.first().review.nextReviewAt)
    }

    @Test
    fun `never-reviewed card maps as due now`() = runTest {
        val deckId = insertDeck()
        insertFlashcard(deckId)

        val before = System.currentTimeMillis()
        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        assertEquals(1, cards.size)
        assertEquals(0L, cards.first().review.lastReviewedAt)
        val nextReviewAt = cards.first().review.nextReviewAt
        assert(nextReviewAt >= before) { "expected nextReviewAt ~now, was $nextReviewAt" }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun insertDeck(): String {
        val id = UUID.randomUUID().toString()
        db.deckQueries.insert(
            id = id,
            name = "Test deck",
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
    }

    private fun insertFlashcard(deckId: String): String {
        val id = UUID.randomUUID().toString()
        db.flashcardQueries.create(
            id = id,
            deckId = deckId,
            word = "word",
            meaning = "meaning",
            translation = null,
            phonetic = null,
            partOfSpeech = null,
            type = null,
            note = null,
            register = null,
            levelBand = null,
            domain = null,
            lemma = null,
            whyUseful = null,
            usagePattern = null,
            irregularFormsJson = null,
            collocationsJson = null,
            commonMistake = null,
            confusableWithJson = null,
            clozeSentence = null,
            sourceContext = null,
            warningsJson = null,
            studyCardsJson = null,
            qualityChecksJson = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
    }

    private fun insertProjection(flashcardId: String, lastReviewedAt: Long, nextReviewAt: Long) {
        db.localFirstQueries.upsertReviewProjection(
            flashcardId = flashcardId,
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            sourceEventId = UUID.randomUUID().toString(),
            updatedAt = lastReviewedAt,
        )
    }
}
