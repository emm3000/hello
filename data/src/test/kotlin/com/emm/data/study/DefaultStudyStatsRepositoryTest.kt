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
    fun `an empty library has neither due reviews nor new cards`() = runTest {
        assertEquals(0, subject.countReviewsDue(Instant.now()))
        assertEquals(0, subject.countNewCards())
    }

    @Test
    fun `countNewCards counts a flashcard with no ReviewProjection row`() = runTest {
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-new", deckId = deckId)

        assertEquals(1, subject.countNewCards())
        assertEquals(0, subject.countReviewsDue(Instant.now()))
    }

    @Test
    fun `countNewCards does not count a soft-deleted flashcard`() = runTest {
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-deleted", deckId = deckId, deleted = true)

        assertEquals(0, subject.countNewCards())
    }

    @Test
    fun `countNewCards excludes un-enriched cards`() = runTest {
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-pending", deckId = deckId, enrichmentStatus = "PENDING")
        insertFlashcard(flashcardId = "card-enriched", deckId = deckId, enrichmentStatus = "ENRICHED")

        assertEquals(1, subject.countNewCards())
    }

    @Test
    fun `countReviewsDue counts cards with nextReviewAt at or before now`() = runTest {
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-2", deckId = deckId)
        insertFlashcard(flashcardId = "card-3", deckId = deckId)

        insertProjection(flashcardId = "card-1", nextReviewAt = nowMillis - 1000)
        insertProjection(flashcardId = "card-2", nextReviewAt = nowMillis - 500)
        insertProjection(flashcardId = "card-3", nextReviewAt = nowMillis + 1000)

        assertEquals(2, subject.countReviewsDue(now))
        assertEquals(0, subject.countNewCards())
    }

    @Test
    fun `countReviewsDue excludes soft-deleted and un-enriched cards`() = runTest {
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-deleted", deckId = deckId, deleted = true)
        insertFlashcard(flashcardId = "card-pending", deckId = deckId, enrichmentStatus = "PENDING")
        insertFlashcard(flashcardId = "card-alive", deckId = deckId)
        insertProjection(flashcardId = "card-deleted", nextReviewAt = nowMillis - 1000)
        insertProjection(flashcardId = "card-pending", nextReviewAt = nowMillis - 1000)
        insertProjection(flashcardId = "card-alive", nextReviewAt = nowMillis - 1000)

        assertEquals(1, subject.countReviewsDue(now))
    }

    @Test
    fun `due reviews and new cards partition the whole due total without overlap`() = runTest {
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()
        val deckId = "deck-a"
        insertDeck(deckId)
        insertFlashcard(flashcardId = "card-new-1", deckId = deckId)
        insertFlashcard(flashcardId = "card-new-2", deckId = deckId)
        insertFlashcard(flashcardId = "card-due", deckId = deckId)
        insertFlashcard(flashcardId = "card-future", deckId = deckId)
        insertFlashcard(flashcardId = "card-deleted", deckId = deckId, deleted = true)
        insertFlashcard(flashcardId = "card-pending", deckId = deckId, enrichmentStatus = "PENDING")
        insertProjection(flashcardId = "card-due", nextReviewAt = nowMillis - 1000)
        insertProjection(flashcardId = "card-future", nextReviewAt = nowMillis + 86400000)
        insertProjection(flashcardId = "card-deleted", nextReviewAt = nowMillis - 1000)

        val uncappedDueTotal: Int = db.localFirstQueries.countCardsDueBy(nowMillis).executeAsOne().toInt()

        assertEquals(3, uncappedDueTotal)
        assertEquals(uncappedDueTotal, subject.countReviewsDue(now) + subject.countNewCards())
    }

    @Test
    fun `countCardsFirstReviewedIn ignores a card whose first review predates the window`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val tomorrowStart = todayStart.plusSeconds(86400)
        val yesterdayStart = todayStart.minusSeconds(86400)

        insertReviewEvent(flashcardId = "card-1", reviewedAt = yesterdayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 5000)

        assertEquals(0, subject.countCardsFirstReviewedIn(todayStart, tomorrowStart))
    }

    @Test
    fun `countCardsFirstReviewedIn counts a card once no matter how often it was reviewed today`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val tomorrowStart = todayStart.plusSeconds(86400)

        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 1000)
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 2000)

        assertEquals(1, subject.countCardsFirstReviewedIn(todayStart, tomorrowStart))
    }

    @Test
    fun `countCardsFirstReviewedIn counts every card introduced inside the window`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val tomorrowStart = todayStart.plusSeconds(86400)
        val yesterdayStart = todayStart.minusSeconds(86400)

        insertReviewEvent(flashcardId = "card-old", reviewedAt = yesterdayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-2", reviewedAt = todayStart.toEpochMilli() + 10_000)
        insertReviewEvent(flashcardId = "card-3", reviewedAt = tomorrowStart.toEpochMilli())

        assertEquals(2, subject.countCardsFirstReviewedIn(todayStart, tomorrowStart))
    }

    @Test
    fun `countCardsFirstReviewedIn returns 0 when nothing was ever reviewed`() = runTest {
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        assertEquals(0, subject.countCardsFirstReviewedIn(todayStart, todayStart.plusSeconds(86400)))
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
            productionSince = null,
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
