package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.ids.toFlashcardId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Regression for the Card Detail "next review" bug: fetchById must surface the flashcard's
 * REAL ReviewProjection (when one exists), not an always-now FsrsCard.new(...) stub.
 */
class DefaultFlashcardRepositoryFetchByIdTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultFlashcardRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultFlashcardRepository(
            db = db,
            json = Json,
            ioDispatcher = Dispatchers.IO,
        )
    }

    @Test
    fun `fetchById surfaces the persisted review projection, not now`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)
        insertProjection(cardId, lastReviewedAt = 1_000L, nextReviewAt = 2_000L)

        val detail = subject.fetchById(cardId.toFlashcardId())

        assertEquals(1_000L, detail.flashcard.review.lastReviewedAt)
        assertEquals(2_000L, detail.flashcard.review.nextReviewAt)
    }

    @Test
    fun `fetchById keeps a fresh FsrsCard for a never-reviewed card`() = runTest {
        val deckId = insertDeck()
        val cardId = insertFlashcard(deckId)

        val before = System.currentTimeMillis()
        val detail = subject.fetchById(cardId.toFlashcardId())

        val review = detail.flashcard.review
        assert(review.nextReviewAt >= before) { "expected nextReviewAt ~now, was ${review.nextReviewAt}" }
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
