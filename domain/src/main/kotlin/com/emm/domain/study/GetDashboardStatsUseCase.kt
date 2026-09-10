package com.emm.domain.study

import com.emm.domain.time.Clock
import com.emm.domain.time.DayRange
import com.emm.domain.time.todayRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private data class DueTodayCount(
    val cards: Int,
    val heldBackNewCards: Int,
)

class GetDashboardStatsUseCase(
    private val repository: StudyStatsRepository,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    suspend operator fun invoke(): DashboardStats {
        val now: Instant = clock.now()

        val cardsStudiedToday: Int = repository.countDistinctCardsStudiedToday()
        val dueToday: DueTodayCount = countDueToday(now)
        val cardsDueThisWeek: Int = repository.countCardsDueThisWeek()
        val currentStreak: Int = computeStreak(now)

        return DashboardStats(
            cardsStudiedToday = cardsStudiedToday,
            cardsDueToday = dueToday.cards,
            currentStreak = currentStreak,
            cardsDueThisWeek = cardsDueThisWeek,
            nextDue = if (dueToday.cards > 0) null else findNextDue(now),
            heldBackNewCards = dueToday.heldBackNewCards,
        )
    }

    private suspend fun countDueToday(now: Instant): DueTodayCount {
        val today: DayRange = clock.todayRange(zone)
        val budget = NewCardBudget(
            introducedToday = repository.countCardsFirstReviewedIn(
                start = today.start,
                endExclusive = today.endExclusive,
            ),
        )
        val newCards: Int = repository.countNewCards()
        val admitted: Int = budget.allow(newCards)

        return DueTodayCount(
            cards = repository.countReviewsDue(now) + admitted,
            heldBackNewCards = newCards - admitted,
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
