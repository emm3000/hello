package com.emm.domain.study

import com.emm.domain.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GetDashboardStatsUseCaseTest {

    private val zone: ZoneId = ZoneId.of("America/Lima")
    private val today: LocalDate = LocalDate.of(2026, 5, 4)
    private val fixedNow: Instant = today.atTime(15, 59).atZone(zone).toInstant()
    private val clock: Clock = Clock { fixedNow }

    @Test
    fun `invoke returns stats from repository`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 5,
            cardsDueToday = 3,
            cardsDueThisWeek = 10,
            reviewTimestamps = listOf(
                reviewAt(today, 15, 0),
                reviewAt(today.minusDays(1), 9, 0),
                reviewAt(today.minusDays(2), 22, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(5, result.cardsStudiedToday)
        assertEquals(3, result.cardsDueToday)
        assertEquals(10, result.cardsDueThisWeek)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `invoke when no reviews returns zero streak`() = runTest {
        val result: DashboardStats = useCase(FakeStatsRepo(reviewTimestamps = emptyList()))()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `invoke when streak broken by missing yesterday returns 1`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 1,
            reviewTimestamps = listOf(
                reviewAt(today, 15, 0),
                reviewAt(today.minusDays(2), 15, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `a streak stays alive through today until it is missed`() = runTest {
        val fakeRepo = FakeStatsRepo(
            reviewTimestamps = listOf(
                reviewAt(today.minusDays(1), 15, 0),
                reviewAt(today.minusDays(2), 15, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `a streak whose last review was two days ago is broken`() = runTest {
        val fakeRepo = FakeStatsRepo(
            reviewTimestamps = listOf(
                reviewAt(today.minusDays(2), 15, 0),
                reviewAt(today.minusDays(3), 15, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `a review today extends a streak that ended yesterday`() = runTest {
        val fakeRepo = FakeStatsRepo(
            reviewTimestamps = listOf(
                reviewAt(today, 15, 0),
                reviewAt(today.minusDays(1), 15, 0),
                reviewAt(today.minusDays(2), 15, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `invoke with long consecutive streak counts correctly`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 3,
            cardsDueToday = 5,
            cardsDueThisWeek = 15,
            reviewTimestamps = listOf(0L, 1L, 2L, 3L, 4L, 5L, 7L)
                .map { dayOffset -> reviewAt(today.minusDays(dayOffset), 15, 0) },
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(6, result.currentStreak)
    }

    @Test
    fun `a review in the afternoon still counts as today west of UTC`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 8,
            reviewTimestamps = listOf(reviewAt(today, 15, 0)),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `a review late at night still counts as today east of UTC`() = runTest {
        val tokyo: ZoneId = ZoneId.of("Asia/Tokyo")
        val nowInTokyo: Instant = today.atTime(1, 30).atZone(tokyo).toInstant()
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 2,
            reviewTimestamps = listOf(today.atTime(1, 0).atZone(tokyo).toInstant().toEpochMilli()),
        )

        val result: DashboardStats = GetDashboardStatsUseCase(fakeRepo, Clock { nowInTokyo }, tokyo)()

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `several reviews on the same local day advance the streak once`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsStudiedToday = 8,
            reviewTimestamps = listOf(
                reviewAt(today, 20, 0),
                reviewAt(today, 15, 0),
                reviewAt(today, 8, 0),
                reviewAt(today.minusDays(1), 19, 0),
                reviewAt(today.minusDays(1), 7, 0),
            ),
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `invoke with nothing due today reports the next batch and the days until it`() = runTest {
        val tomorrowMorning: Long = reviewAt(today.plusDays(1), 9, 0)
        val fakeRepo = FakeStatsRepo(
            cardsDueToday = 0,
            nextReviewAt = tomorrowMorning,
            cardsDueInRange = 5,
        )

        val result: DashboardStats = useCase(fakeRepo)()

        val nextDue: NextDueBatch? = result.nextDue
        assertEquals(Instant.ofEpochMilli(tomorrowMorning), nextDue?.at)
        assertEquals(5, nextDue?.cardCount)
        assertEquals(1, nextDue?.daysFromToday)
    }

    @Test
    fun `invoke counts the next batch only over the day it falls on`() = runTest {
        val nextDay: LocalDate = today.plusDays(1)
        val nextReviewAt: Long = reviewAt(nextDay, 9, 0)
        val fakeRepo = FakeStatsRepo(
            cardsDueToday = 0,
            nextReviewAt = nextReviewAt,
            cardsDueInRange = 2,
        )

        useCase(fakeRepo)()

        assertEquals(nextReviewAt, fakeRepo.rangeStartMillis)
        assertEquals(
            nextDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            fakeRepo.rangeEndMillis,
        )
    }

    @Test
    fun `invoke with cards due today never looks up the next batch`() = runTest {
        val fakeRepo = FakeStatsRepo(cardsDueToday = 3, nextReviewAt = 1L, cardsDueInRange = 9)

        val result: DashboardStats = useCase(fakeRepo)()

        assertNull(result.nextDue)
        assertTrue(!fakeRepo.nextReviewAtWasQueried)
    }

    @Test
    fun `invoke with nothing scheduled at all reports no next batch`() = runTest {
        val fakeRepo = FakeStatsRepo(cardsDueToday = 0, nextReviewAt = null)

        val result: DashboardStats = useCase(fakeRepo)()

        assertNull(result.nextDue)
    }

    @Test
    fun `invoke drops a next batch that resolves to zero cards`() = runTest {
        val fakeRepo = FakeStatsRepo(
            cardsDueToday = 0,
            nextReviewAt = reviewAt(today.plusDays(1), 9, 0),
            cardsDueInRange = 0,
        )

        val result: DashboardStats = useCase(fakeRepo)()

        assertNull(result.nextDue)
    }

    private fun useCase(repository: StudyStatsRepository): GetDashboardStatsUseCase {
        return GetDashboardStatsUseCase(repository, clock, zone)
    }

    private fun reviewAt(date: LocalDate, hour: Int, minute: Int): Long {
        return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
    }

    private class FakeStatsRepo(
        private val cardsStudiedToday: Int = 0,
        private val cardsDueToday: Int = 0,
        private val cardsDueThisWeek: Int = 0,
        private val cardsDueInRange: Int = 0,
        private val nextReviewAt: Long? = null,
        private val reviewTimestamps: List<Long> = emptyList(),
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

        override suspend fun findReviewTimestampsDescending(): List<Long> = reviewTimestamps
    }
}
