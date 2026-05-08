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

        // Same card reviewed 3 times today
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli())
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 1000)
        insertReviewEvent(flashcardId = "card-1", reviewedAt = todayStart.toEpochMilli() + 2000)
        // Different card reviewed today
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
    fun `countCardsDueToday returns 0 when no projections exist`() = runTest {
        assertEquals(0, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueToday counts cards with nextReviewAt at or before now`() = runTest {
        val now = Instant.now().toEpochMilli()

        insertProjection(flashcardId = "card-1", nextReviewAt = now - 1000)
        insertProjection(flashcardId = "card-2", nextReviewAt = now - 500)
        insertProjection(flashcardId = "card-3", nextReviewAt = now + 1000)

        assertEquals(2, subject.countCardsDueToday())
    }

    @Test
    fun `countCardsDueThisWeek counts cards due in next 7 days`() = runTest {
        val now = Instant.now().toEpochMilli()
        val threeDays = 3L * 86400 * 1000
        val eightDays = 8L * 86400 * 1000

        insertProjection(flashcardId = "card-1", nextReviewAt = now + 1000)
        insertProjection(flashcardId = "card-2", nextReviewAt = now + threeDays)
        insertProjection(flashcardId = "card-3", nextReviewAt = now + eightDays)
        insertProjection(flashcardId = "card-4", nextReviewAt = now - 1000)

        assertEquals(2, subject.countCardsDueThisWeek())
    }

    @Test
    fun `findDistinctReviewDatesDescending returns dates in order`() = runTest {
        // Insert events at specific millis; SQL truncates to UTC day boundaries
        val may4 = utcDayMillis("2026-05-04")
        val may3 = utcDayMillis("2026-05-03")
        val may2 = utcDayMillis("2026-05-02")

        insertReviewEvent(flashcardId = "card-1", reviewedAt = may4)
        insertReviewEvent(flashcardId = "card-2", reviewedAt = may4 + 1000) // same UTC day
        insertReviewEvent(flashcardId = "card-1", reviewedAt = may3)
        insertReviewEvent(flashcardId = "card-3", reviewedAt = may2)

        val dates = subject.findDistinctReviewDatesDescending()

        assertEquals(3, dates.size)
        assertEquals(may4, dates[0])
        assertEquals(may3, dates[1])
        assertEquals(may2, dates[2])
    }

    @Test
    fun `findDistinctReviewDatesDescending returns empty when no reviews`() = runTest {
        val dates = subject.findDistinctReviewDatesDescending()
        assertEquals(0, dates.size)
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
        )
    }

    private fun insertProjection(flashcardId: String, nextReviewAt: Long) {
        db.localFirstQueries.upsertReviewProjection(
            flashcardId = flashcardId,
            lastReviewedAt = nextReviewAt - 86400000,
            nextReviewAt = nextReviewAt,
            easeFactor = 2.5,
            interval = 1,
            repetitions = 1,
            lapses = 0,
            sourceEventId = UUID.randomUUID().toString(),
            updatedAt = nextReviewAt,
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
