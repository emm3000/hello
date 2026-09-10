package com.emm.domain.study

import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.DeckId
import com.emm.domain.time.Clock
import com.emm.domain.time.DayRange
import com.emm.domain.time.todayRange
import java.time.ZoneId
import kotlin.random.Random

class GetStudySessionUseCase(
    private val studySessionRepository: StudySessionRepository,
    private val studyStatsRepository: StudyStatsRepository,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val random: Random = Random.Default,
) {

    suspend operator fun invoke(deckId: DeckId?, extraNewCards: Int = 0): StudySession {
        val dueCards: List<StudyFlashcard> = fetchSession(deckId)
        val (newCards: List<StudyFlashcard>, reviews: List<StudyFlashcard>) =
            dueCards.partition { it.review.state == FsrsState.NEW }

        val budget: NewCardBudget = budgetFor(extraNewCards)
        val introduced: Int = budget.allow(newCards.size)

        return StudySession(
            cards = reviews.shuffled(random) + newCards.shuffled(random).take(introduced),
            heldBackNewCards = newCards.size - introduced,
        )
    }

    private suspend fun budgetFor(extraNewCards: Int): NewCardBudget {
        val budget = NewCardBudget(introducedToday = countIntroducedToday())
        return if (extraNewCards > 0) budget.extendedBy(extraNewCards) else budget
    }

    private suspend fun fetchSession(deckId: DeckId?): List<StudyFlashcard> = when (deckId) {
        null -> studySessionRepository.sessionTodayAllDecks()
        else -> studySessionRepository.sessionToday(deckId)
    }

    private suspend fun countIntroducedToday(): Int {
        val today: DayRange = clock.todayRange(zone)
        return studyStatsRepository.countCardsFirstReviewedIn(
            start = today.start,
            endExclusive = today.endExclusive,
        )
    }
}
