package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Regression for the deck-detail crash: cards whose stored nextReviewAt is already in the
 * past must map without violating the FsrsCard nextReviewAt >= lastReviewedAt invariant.
 */
class FlashcardWithReviewMappingTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultStudySessionRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultStudySessionRepository(
            db = db,
            json = Json,
            ioDispatcher = Dispatchers.IO,
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
        // No ReviewProjection row: falls back to FsrsCard.new(...), which stamps BOTH
        // lastReviewedAt and nextReviewAt with "now" (see mapFsrsCard's null-guard).
        val review = cards.first().review
        assert(review.lastReviewedAt >= before) { "expected lastReviewedAt ~now, was ${review.lastReviewedAt}" }
        assert(review.nextReviewAt >= before) { "expected nextReviewAt ~now, was ${review.nextReviewAt}" }
    }

    @Test
    fun `reviewed card retains real FSRS fields, not a permanently NEW stub`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)
        insertProjection(cardId, lastReviewedAt = 1_000L, nextReviewAt = 2_000L)

        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        val review = cards.first().review
        assertEquals(FsrsState.REVIEW, review.state)
        assertEquals(1.0, review.stability, 0.0)
        assertEquals(5.0, review.difficulty, 0.0)
        assertEquals(1L, review.interval)
        assertEquals(1L, review.reps)
        assertEquals(0L, review.lapses)
    }

    @Test
    fun `flashcard with one example surfaces it on the study flashcard`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)
        insertExample(
            flashcardId = cardId,
            text = "She showed great compassion.",
            translation = "Ella mostró gran compasión.",
            createdAt = 100L,
        )

        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        assertEquals(1, cards.size)
        assertEquals("She showed great compassion.", cards.first().example)
        assertEquals("Ella mostró gran compasión.", cards.first().exampleTranslation)
    }

    @Test
    fun `two examples still yield one study flashcard, both fields from the earliest example`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)
        insertExample(
            flashcardId = cardId,
            text = "Later example.",
            translation = "Ejemplo posterior.",
            createdAt = 200L,
        )
        insertExample(
            flashcardId = cardId,
            text = "Earlier example.",
            translation = "Ejemplo anterior.",
            createdAt = 100L,
        )

        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        assertEquals(1, cards.size)
        assertEquals("Earlier example.", cards.first().example)
        assertEquals("Ejemplo anterior.", cards.first().exampleTranslation)
    }

    @Test
    fun `flashcard with no examples maps example fields to empty strings, not null`() = runTest {
        val deckId = insertDeck()
        insertFlashcard(deckId)

        val cards = subject.flashcardWithReview(DeckId.from(deckId)).first()

        assertEquals(1, cards.size)
        assertEquals("", cards.first().example)
        assertEquals("", cards.first().exampleTranslation)
    }

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
            enrichmentStatus = "ENRICHED",
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
    }

    private fun insertExample(
        flashcardId: String,
        text: String,
        translation: String,
        createdAt: Long,
    ): String {
        val id = UUID.randomUUID().toString()
        db.flashcardExampleQueries.insert(
            id = id,
            flashcardId = flashcardId,
            text = text,
            translation = translation,
            type = "sentence",
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null,
        )
        return id
    }

    private fun insertProjection(flashcardId: String, lastReviewedAt: Long, nextReviewAt: Long) {
        db.localFirstQueries.insertReviewProjectionFull(
            flashcardId = flashcardId,
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            sourceEventId = UUID.randomUUID().toString(),
            updatedAt = lastReviewedAt,
            state = "REVIEW",
            stability = 1.0,
            difficulty = 5.0,
            productionSince = null,
        )
    }
}
