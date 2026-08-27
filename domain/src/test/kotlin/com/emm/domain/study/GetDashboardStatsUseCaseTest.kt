package com.emm.domain.study

import com.emm.domain.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetDashboardStatsUseCaseTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val fixedNow: Instant = Instant.parse("2026-05-04T12:00:00Z")
    private val clock: Clock = Clock { fixedNow }

    @Test
    fun `invoke returns stats from repository`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 5,
            cardsDueToday = 3,
            cardsDueThisWeek = 10,
            reviewDates = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            ),
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(5, result.cardsStudiedToday)
        assertEquals(3, result.cardsDueToday)
        assertEquals(10, result.cardsDueThisWeek)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `invoke when no reviews returns zero streak`() = runTest {
        val fakeRepo = FakeStatsRepo(reviewDates = emptyList())

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `invoke when streak broken by missing yesterday returns 1`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 1,
            reviewDates = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            ),
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `invoke when no reviews today but yesterday exists returns 0 streak`() = runTest {
        val fakeRepo = FakeStatsRepo(
            reviewDates = listOf(
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            ),
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `invoke with long consecutive streak counts correctly`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 3,
            cardsDueToday = 5,
            cardsDueThisWeek = 15,
            reviewDates = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
                dateToMillis(LocalDate.of(2026, 5, 1)),
                dateToMillis(LocalDate.of(2026, 4, 30)),
                dateToMillis(LocalDate.of(2026, 4, 29)),
                dateToMillis(LocalDate.of(2026, 4, 27)),
            ),
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(6, result.currentStreak)
    }

    @Test
    fun `invoke with nothing due today reports the next batch and the days until it`() = runTest {
        val tomorrowMorning: Long = LocalDate.of(2026, 5, 5)
            .atTime(9, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val fakeRepo = FakeStatsRepo(
            cardsDueToday = 0,
            nextReviewAt = tomorrowMorning,
            cardsDueInRange = 5,
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        val nextDue: NextDueBatch? = result.nextDue
        assertEquals(Instant.ofEpochMilli(tomorrowMorning), nextDue?.at)
        assertEquals(5, nextDue?.cardCount)
        assertEquals(1, nextDue?.daysFromToday)
    }

    @Test
    fun `invoke counts the next batch only over the day it falls on`() = runTest {
        val nextDayStart: LocalDate = LocalDate.of(2026, 5, 5)
        val nextReviewAt: Long = nextDayStart.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val fakeRepo = FakeStatsRepo(
            cardsDueToday = 0,
            nextReviewAt = nextReviewAt,
            cardsDueInRange = 2,
        )

        GetDashboardStatsUseCase(fakeRepo, clock)()

        assertEquals(nextReviewAt, fakeRepo.rangeStartMillis)
        assertEquals(
            nextDayStart.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            fakeRepo.rangeEndMillis,
        )
    }

    @Test
    fun `invoke with cards due today never looks up the next batch`() = runTest {
        val fakeRepo = FakeStatsRepo(cardsDueToday = 3, nextReviewAt = 1L, cardsDueInRange = 9)

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertNull(result.nextDue)
        assertTrue(!fakeRepo.nextReviewAtWasQueried)
    }

    @Test
    fun `invoke with nothing scheduled at all reports no next batch`() = runTest {
        val fakeRepo = FakeStatsRepo(cardsDueToday = 0, nextReviewAt = null)

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertNull(result.nextDue)
    }

    @Test
    fun `invoke drops a next batch that resolves to zero cards`() = runTest {
        val tomorrow: Long = LocalDate.of(2026, 5, 5)
            .atTime(9, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val fakeRepo = FakeStatsRepo(cardsDueToday = 0, nextReviewAt = tomorrow, cardsDueInRange = 0)

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, clock)()

        assertNull(result.nextDue)
    }

    private fun dateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private class FakeStatsRepo(
        private val cardsStudiedToday: Int = 0,
        private val cardsDueToday: Int = 0,
        private val cardsDueThisWeek: Int = 0,
        private val cardsDueInRange: Int = 0,
        private val nextReviewAt: Long? = null,
        private val reviewDates: List<Long> = emptyList(),
    ) : StudyStatsRepository {

        var nextReviewAtWasQueried: Boolean = false
            private set

        var rangeStartMillis: Long? = null
            private set

        var rangeEndMillis: Long? = null
            private set

        override suspend fun countDistinctCardsStudiedToday(): Int = cardsStudiedToday

        override suspend fun countCardsDueToday(): Int = cardsDueToday

        override suspend fun countCardsDueThisWeek(): Int = cardsDueThisWeek

        override suspend fun countCardsDueInRange(startMillis: Long, endMillis: Long): Int {
            rangeStartMillis = startMillis
            rangeEndMillis = endMillis
            return cardsDueInRange
        }

        override suspend fun findNextReviewAtAfter(millis: Long): Long? {
            nextReviewAtWasQueried = true
            return nextReviewAt
        }

        override suspend fun findDistinctReviewDatesDescending(): List<Long> = reviewDates
    }
}
