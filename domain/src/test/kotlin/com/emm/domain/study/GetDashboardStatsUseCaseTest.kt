package com.emm.domain.study

import com.emm.domain.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetDashboardStatsUseCaseTest {

    private val zone = ZoneId.systemDefault()

    @Test
    fun `invoke returns stats from repository`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }
        val fakeRepo = object : StudyStatsRepository {
            override suspend fun countDistinctCardsStudiedToday() = 5
            override suspend fun countCardsDueToday() = 3
            override suspend fun countCardsDueThisWeek() = 10
            override suspend fun findDistinctReviewDatesDescending() = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            )
        }

        val useCase = GetDashboardStatsUseCase(fakeRepo, clock)
        val result = useCase()

        assertEquals(5, result.cardsStudiedToday)
        assertEquals(3, result.cardsDueToday)
        assertEquals(10, result.cardsDueThisWeek)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `invoke when no reviews returns zero streak`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }
        val fakeRepo = object : StudyStatsRepository {
            override suspend fun countDistinctCardsStudiedToday() = 0
            override suspend fun countCardsDueToday() = 0
            override suspend fun countCardsDueThisWeek() = 0
            override suspend fun findDistinctReviewDatesDescending() = emptyList<Long>()
        }

        val useCase = GetDashboardStatsUseCase(fakeRepo, clock)
        val result = useCase()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `invoke when streak broken by missing yesterday returns 1`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }
        val fakeRepo = object : StudyStatsRepository {
            override suspend fun countDistinctCardsStudiedToday() = 1
            override suspend fun countCardsDueToday() = 0
            override suspend fun countCardsDueThisWeek() = 0

            // Only today and 2 days ago — yesterday is missing
            override suspend fun findDistinctReviewDatesDescending() = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            )
        }

        val useCase = GetDashboardStatsUseCase(fakeRepo, clock)
        val result = useCase()

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `invoke when no reviews today but yesterday exists returns 0 streak`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }
        val fakeRepo = object : StudyStatsRepository {
            override suspend fun countDistinctCardsStudiedToday() = 0
            override suspend fun countCardsDueToday() = 0
            override suspend fun countCardsDueThisWeek() = 0

            // Only yesterday, not today
            override suspend fun findDistinctReviewDatesDescending() = listOf(
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
            )
        }

        val useCase = GetDashboardStatsUseCase(fakeRepo, clock)
        val result = useCase()

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `invoke with long consecutive streak counts correctly`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }
        val fakeRepo = object : StudyStatsRepository {
            override suspend fun countDistinctCardsStudiedToday() = 3
            override suspend fun countCardsDueToday() = 5
            override suspend fun countCardsDueThisWeek() = 15
            override suspend fun findDistinctReviewDatesDescending() = listOf(
                dateToMillis(LocalDate.of(2026, 5, 4)),
                dateToMillis(LocalDate.of(2026, 5, 3)),
                dateToMillis(LocalDate.of(2026, 5, 2)),
                dateToMillis(LocalDate.of(2026, 5, 1)),
                dateToMillis(LocalDate.of(2026, 4, 30)),
                dateToMillis(LocalDate.of(2026, 4, 29)),
                dateToMillis(LocalDate.of(2026, 4, 27)), // gap here
            )
        }

        val useCase = GetDashboardStatsUseCase(fakeRepo, clock)
        val result = useCase()

        assertEquals(6, result.currentStreak)
    }

    private fun dateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
