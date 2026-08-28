package com.emm.domain.study

import com.emm.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class GetDashboardStatsUseCase(
    private val repository: StudyStatsRepository,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    suspend operator fun invoke(): DashboardStats {
        val now: Instant = clock.now()

        val cardsStudiedToday = repository.countDistinctCardsStudiedToday()
        val cardsDueToday = repository.countCardsDueToday()
        val cardsDueThisWeek = repository.countCardsDueThisWeek()
        val currentStreak = computeStreak(now)

        return DashboardStats(
            cardsStudiedToday = cardsStudiedToday,
            cardsDueToday = cardsDueToday,
            currentStreak = currentStreak,
            cardsDueThisWeek = cardsDueThisWeek,
            nextDue = if (cardsDueToday > 0) null else findNextDue(now),
        )
    }

    private suspend fun findNextDue(now: Instant): NextDueBatch? {
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

    private suspend fun computeStreak(now: Instant): Int {
        val reviewDates: List<LocalDate> = repository.findReviewTimestampsDescending()
            .map { reviewedAtMillis -> Instant.ofEpochMilli(reviewedAtMillis).atZone(zone).toLocalDate() }
            .distinct()
        if (reviewDates.isEmpty()) return 0

        val today: LocalDate = now.atZone(zone).toLocalDate()
        val yesterday: LocalDate = today.minusDays(1)
        val latest: LocalDate = reviewDates.first()
        if (latest != today && latest != yesterday) return 0

        return countConsecutiveDays(reviewDates, latest)
    }

    private fun countConsecutiveDays(reviewDates: List<LocalDate>, startDate: LocalDate): Int {
        var streak: Int = 0
        var expectedDate: LocalDate = startDate

        for (reviewDate in reviewDates) {
            if (reviewDate == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (reviewDate.isBefore(expectedDate)) {
                break
            }
        }

        return streak
    }
}
