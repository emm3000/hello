package com.emm.data.study

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DefaultStudyStatsRepositoryTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultStudyStatsRepository

    private val zone = ZoneId.systemDefault()

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultStudyStatsRepository(db)
    }

    @Test
    fun `countDistinctCardsStudiedToday returns 0 when no reviews exist`() = runTest {
        assertEquals(0, subject.countDistinctCardsStudiedToday())
    }

    @Test
    fun `countDistinctCardsStudiedToday counts distinct cards only`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 1000)
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 2000)
        insertReviewEvent(flashcardId = "card-2", reviewedAt = todayStart.toEpochMilli())

        assertEquals(2, subject.countDistinctCardsStudiedToday())
    }

    @Test
    fun `countDistinctCardsStudiedToday excludes yesterday reviews`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val yesterdayStart = todayStart.minusSeconds(86400)

        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-2", reviewedAt = yesterdayStart.toEpochMilli())

        assertEquals(1, subject.countDistinctCardsStudiedToday())
    }

    @Test
    fun `countCardsDueToday returns 0 when no flashcards exist`() = runTest {
        assertEquals(0, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday counts never-reviewed flashcard with no ReviewProjection row`() = runTest {
        // A brand-new card has no ReviewProjection. NULL nextReviewAt means due now.
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-new", deckId = deckId)

        assertEquals(1, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday does not count soft-deleted flashcard with no ReviewProjection row`() = runTest {
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-deleted", deckId = deckId, deleted = true)

        assertEquals(0, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday counts cards with nextReviewAt at or before now`() = runTest {
        val now = Instant.now().toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-2", deckId = deckId)
        insertFlashcard(flashcardId = "card-3", deckId = deckId)

        insertProjection(flashcardId = "card-1", nextReviewAt = now - 1000)
        insertProjection(flashcardId = "card-2", nextReviewAt = now - 500)
        insertProjection(flashcardId = "card-3", nextReviewAt = now + 1000)

        assertEquals(2, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday counts never-reviewed card alongside reviewed due cards`() = runTest {
        val now = Instant.now().toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-2", deckId = deckId)
        insertProjection(flashcardId = "card-2", nextReviewAt = now - 1000)
        insertFlashcard(flashcardId = "card-3", deckId = deckId)
        insertProjection(flashcardId = "card-3", nextReviewAt = now + 86400000)

        assertEquals(2, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday excludes un-enriched cards`() = runTest {
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-pending", deckId = deckId, enrichmentStatus = "PENDING")
        insertFlashcard(flashcardId = "card-enriched", deckId = deckId, enrichmentStatus = "ENRICHED")

        assertEquals(1, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueThisWeek excludes un-enriched cards`() = runTest {
        val now = Instant.now().toEpochMilli()
        val threeDays = 3L * 86400 * 1000
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-pending", deckId = deckId, enrichmentStatus = "PENDING")
        insertFlashcard(flashcardId = "card-enriched", deckId = deckId, enrichmentStatus = "ENRICHED")
        insertProjection(flashcardId = "card-pending", nextReviewAt = now + threeDays)
        insertProjection(flashcardId = "card-enriched", nextReviewAt = now + threeDays)

        assertEquals(1, subject.countCardsDueThisWeek())
    }

    @Test
    fun `countCardsDueThisWeek counts cards due in next 7 days`() = runTest {
        val now = Instant.now().toEpochMilli()
        val threeDays = 3L * 86400 * 1000
        val eightDays = 8L * 86400 * 1000
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-2", deckId = deckId)
        insertFlashcard(flashcardId = "card-3", deckId = deckId)
        insertFlashcard(flashcardId = "card-4", deckId = deckId)

        insertProjection(flashcardId = "card-1", nextReviewAt = now + 1000)
        insertProjection(flashcardId = "card-2", nextReviewAt = now + threeDays)
        insertProjection(flashcardId = "card-3", nextReviewAt = now + eightDays)
        // card-4 is overdue (past), not in the [now, now+7d) future window
        insertProjection(flashcardId = "card-4", nextReviewAt = now - 1000)

        assertEquals(2, subject.countCardsDueThisWeek())
    }

    @Test
    fun `countCardsDueThisWeek does not count never-reviewed cards (overdue, not in future window)`() = runTest {
        val now = Instant.now().toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        // card-1: no ReviewProjection; due now (overdue), NOT in future window [now, now+7d)
        insertFlashcard(flashcardId = "card-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-2", deckId = deckId)
        insertProjection(flashcardId = "card-2", nextReviewAt = now + 86400000)

        assertEquals(1, subject.countCardsDueThisWeek())
    }

    @Test
    fun `findReviewTimestampsDescending returns every raw timestamp newest first`() = runTest {
        val may4 = utcDayMillis("2026-05-04")
        val may3 = utcDayMillis("2026-05-03")
        val may2 = utcDayMillis("2026-05-02")

        insertReviewEvent(flashcardId = "card-1", reviewedAt = may4)
        insertReviewEvent(flashcardId = "card-2", reviewedAt = may4 + 1000)
        insertReviewEvent(flashcardId = "card-1", reviewedAt = may3)
        insertReviewEvent(flashcardId = "card-3", reviewedAt = may2)

        val timestamps = subject.findReviewTimestampsDescending()

        assertEquals(listOf(may4 + 1000, may4, may3, may2), timestamps)
    }

    @Test
    fun `findReviewTimestampsDescending returns empty when no reviews`() = runTest {
        val timestamps = subject.findReviewTimestampsDescending()
        assertEquals(0, timestamps.size)
    }

    private fun insertDeck(deckId: String) {
        db.deckQueries.insert(
            id = deckId,
            name = "Test deck",
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
    }

    private fun insertFlashcard(
        flashcardId: String,
        deckId: String,
        deleted: Boolean = false,
        enrichmentStatus: String = "ENRICHED",
    ) {
        db.flashcardQueries.create(
            id = flashcardId,
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
            enrichmentStatus = enrichmentStatus,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = if (deleted) 1L else null,
        )
    }

    private fun insertReviewEvent(flashcardId: String, reviewedAt: Long) {
        db.localFirstQueries.insertReviewEvent(
            eventId = UUID.randomUUID().toString(),
            flashcardId = flashcardId,
            grade = "review",
            reviewedAt = reviewedAt,
            nextReviewAt = reviewedAt + 86400000,
            easeFactor = 2.5,
            interval = 1,
            repetitions = 1,
            lapses = 0,
            createdAt = reviewedAt,
            rating = 0L,
        )
    }

    private fun insertProjection(flashcardId: String, nextReviewAt: Long) {
        db.localFirstQueries.insertReviewProjectionFull(
            flashcardId = flashcardId,
            lastReviewedAt = nextReviewAt - 86400000,
            nextReviewAt = nextReviewAt,
            easeFactor = 2.5,
            interval = 1,
            repetitions = 1,
            lapses = 0,
            sourceEventId = UUID.randomUUID().toString(),
            updatedAt = nextReviewAt,
            state = "REVIEW",
            stability = 1.0,
            difficulty = 5.0,
        )
    }

    /**
     * Computes UTC day boundary millis the same way the SQL query does:
     * (millis / 86400000) * 86400000
     */
    private fun utcDayMillis(date: String): Long {
        val instant = LocalDate.parse(date).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val day = instant.toEpochMilli() / 86400000
        return day * 86400000
    }
}
