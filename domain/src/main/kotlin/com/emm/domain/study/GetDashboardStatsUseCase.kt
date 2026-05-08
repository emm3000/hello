package com.emm.domain.study

import com.emm.domain.time.Clock
import java.time.Instant
import java.time.ZoneId

class GetDashboardStatsUseCase(
    private val repository: StudyStatsRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(): DashboardStats {
        val now = clock.now()
        val zone = ZoneId.systemDefault()

        val cardsStudiedToday = repository.countDistinctCardsStudiedToday()
        val cardsDueToday = repository.countCardsDueToday()
        val cardsDueThisWeek = repository.countCardsDueThisWeek()
        val currentStreak = computeStreak(now, zone)

        return DashboardStats(
            cardsStudiedToday = cardsStudiedToday,
            cardsDueToday = cardsDueToday,
            currentStreak = currentStreak,
            cardsDueThisWeek = cardsDueThisWeek,
        )
    }

    private suspend fun computeStreak(now: Instant, zone: ZoneId): Int {
        val reviewDates = repository.findDistinctReviewDatesDescending()
        if (reviewDates.isEmpty()) return 0

        val today = now.atZone(zone).toLocalDate()
        var streak = 0
        var expectedDate = today

        for (reviewedAtMillis in reviewDates) {
            val reviewDate = Instant.ofEpochMilli(reviewedAtMillis)
                .atZone(zone)
                .toLocalDate()

            if (reviewDate == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (reviewDate.isBefore(expectedDate)) {
                // Gap found — streak breaks
                break
            }
            // If reviewDate is after expectedDate, skip (already counted)
        }

        return streak
    }
}
