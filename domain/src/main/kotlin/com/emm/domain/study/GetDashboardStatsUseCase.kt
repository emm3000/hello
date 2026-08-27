package com.emm.domain.study

import com.emm.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
            nextDue = if (cardsDueToday > 0) null else findNextDue(now, zone),
        )
    }

    private suspend fun findNextDue(now: Instant, zone: ZoneId): NextDueBatch? {
        val nextReviewAtMillis: Long = repository.findNextReviewAtAfter(now.toEpochMilli()) ?: return null
        val at: Instant = Instant.ofEpochMilli(nextReviewAtMillis)
        val dueDate: LocalDate = at.atZone(zone).toLocalDate()
        val endOfDueDate: Instant = dueDate.plusDays(1).atStartOfDay(zone).toInstant()

        val cardCount: Int = repository.countCardsDueInRange(
            startMillis = nextReviewAtMillis,
            endMillis = endOfDueDate.toEpochMilli(),
        )
        if (cardCount <= 0) return null

        return NextDueBatch(
            at = at,
            cardCount = cardCount,
            daysFromToday = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), dueDate).toInt(),
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
