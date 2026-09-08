package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetStudySessionUseCaseTest {

    private val zone: ZoneId = ZoneId.of("America/Lima")
    private val today: LocalDate = LocalDate.of(2026, 5, 4)
    private val fixedNow: Instant = today.atTime(15, 59).atZone(zone).toInstant()
    private val clock: Clock = Clock { fixedNow }

    @Test
    fun `an empty session stays empty`() = runTest {
        val result: List<StudyFlashcard> = useCase(FakeSessionRepo(emptyList()), FakeStatsRepo())(null)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `due reviews always come before never-reviewed cards`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(4) + newCards(4))

        val result: List<StudyFlashcard> = useCase(sessionRepo, FakeStatsRepo())(null)

        assertEquals(8, result.size)
        assertEquals(setOf("r0", "r1", "r2", "r3"), result.take(4).map { it.word }.toSet())
        assertEquals(setOf("n0", "n1", "n2", "n3"), result.drop(4).map { it.word }.toSet())
    }

    @Test
    fun `an interleaved session is still partitioned reviews first`() = runTest {
        val interleaved: List<StudyFlashcard> = listOf(
            newCard("n0"),
            review("r0"),
            newCard("n1"),
            review("r1"),
        )

        val result: List<StudyFlashcard> = useCase(FakeSessionRepo(interleaved), FakeStatsRepo())(null)

        assertEquals(
            listOf(FsrsState.REVIEW, FsrsState.REVIEW, FsrsState.NEW, FsrsState.NEW),
            result.map { it.review.state },
        )
    }

    @Test
    fun `new cards are capped at the daily limit`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(3) + newCards(25))

        val result: List<StudyFlashcard> = useCase(sessionRepo, FakeStatsRepo())(null)

        assertEquals(3 + DEFAULT_DAILY_NEW_CARD_LIMIT, result.size)
        assertEquals(DEFAULT_DAILY_NEW_CARD_LIMIT, result.count { it.review.state == FsrsState.NEW })
    }

    @Test
    fun `cards already introduced today shrink the new card allowance`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(3) + newCards(25))

        val result: List<StudyFlashcard> = useCase(sessionRepo, FakeStatsRepo(firstReviewedInRange = 4))(null)

        assertEquals(6, result.count { it.review.state == FsrsState.NEW })
        assertEquals(9, result.size)
    }

    @Test
    fun `a spent daily allowance introduces no new card at all`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(3) + newCards(25))

        val result: List<StudyFlashcard> = useCase(sessionRepo, FakeStatsRepo(firstReviewedInRange = 10))(null)

        assertEquals(3, result.size)
        assertEquals(0, result.count { it.review.state == FsrsState.NEW })
    }

    @Test
    fun `fewer new cards than the allowance introduces all of them`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(3) + newCards(2))

        val result: List<StudyFlashcard> = useCase(sessionRepo, FakeStatsRepo())(null)

        assertEquals(5, result.size)
        assertEquals(2, result.count { it.review.state == FsrsState.NEW })
    }

    @Test
    fun `the daily allowance is measured over the local calendar day`() = runTest {
        val statsRepo = FakeStatsRepo()

        useCase(FakeSessionRepo(newCards(1)), statsRepo)(null)

        assertEquals(today.atStartOfDay(zone).toInstant(), statsRepo.firstReviewedStart)
        assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant(), statsRepo.firstReviewedEndExclusive)
    }

    @Test
    fun `another seed reorders within each group but keeps the group boundary`() = runTest {
        val cards: List<StudyFlashcard> = reviews(4) + newCards(4)

        val first: List<StudyFlashcard> = useCase(FakeSessionRepo(cards), FakeStatsRepo(), Random(42))(null)
        val second: List<StudyFlashcard> = useCase(FakeSessionRepo(cards), FakeStatsRepo(), Random(7))(null)

        assertNotEquals(first.map { it.word }, second.map { it.word })
        assertEquals(first.take(4).map { it.word }.toSet(), second.take(4).map { it.word }.toSet())
        assertEquals(first.drop(4).map { it.word }.toSet(), second.drop(4).map { it.word }.toSet())
    }

    @Test
    fun `the same seed always produces the same session`() = runTest {
        val cards: List<StudyFlashcard> = reviews(4) + newCards(4)

        val first: List<StudyFlashcard> = useCase(FakeSessionRepo(cards), FakeStatsRepo(), Random(42))(null)
        val second: List<StudyFlashcard> = useCase(FakeSessionRepo(cards), FakeStatsRepo(), Random(42))(null)

        assertEquals(first.map { it.word }, second.map { it.word })
    }

    @Test
    fun `a null deck reads the all-decks session`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(2))

        useCase(sessionRepo, FakeStatsRepo())(null)

        assertTrue(sessionRepo.allDecksCalled)
        assertNull(sessionRepo.sessionTodayCalledWith)
    }

    @Test
    fun `a deck id reads that deck's session`() = runTest {
        val sessionRepo = FakeSessionRepo(reviews(2))

        useCase(sessionRepo, FakeStatsRepo())("deck-1".toDeckId())

        assertEquals("deck-1", sessionRepo.sessionTodayCalledWith?.value)
        assertTrue(!sessionRepo.allDecksCalled)
    }

    private fun useCase(
        sessionRepository: StudySessionRepository,
        statsRepository: StudyStatsRepository,
        random: Random = Random(42),
    ): GetStudySessionUseCase = GetStudySessionUseCase(
        studySessionRepository = sessionRepository,
        studyStatsRepository = statsRepository,
        clock = clock,
        zone = zone,
        random = random,
    )

    private fun reviews(count: Int): List<StudyFlashcard> = List(count) { index -> review("r$index") }

    private fun newCards(count: Int): List<StudyFlashcard> = List(count) { index -> newCard("n$index") }

    private fun review(word: String): StudyFlashcard = studyFlashcard(word, FsrsState.REVIEW)

    private fun newCard(word: String): StudyFlashcard = studyFlashcard(word, FsrsState.NEW)

    private fun studyFlashcard(word: String, state: FsrsState): StudyFlashcard = StudyFlashcard(
        flashcardId = word.toFlashcardId(),
        word = word,
        phonetic = "",
        meaning = "",
        translation = "",
        review = FsrsCard(
            flashcardId = word.toFlashcardId(),
            state = state,
            stability = if (state == FsrsState.NEW) 0.0 else 1.0,
            difficulty = if (state == FsrsState.NEW) 0.0 else 5.0,
            lastReviewedAt = 0L,
            nextReviewAt = 0L,
            interval = 0L,
            reps = 0L,
            lapses = 0L,
        ),
        studyCards = emptyList(),
    )

    private class FakeSessionRepo(
        private val cards: List<StudyFlashcard>,
    ) : StudySessionRepository {

        var sessionTodayCalledWith: DeckId? = null
            private set

        var allDecksCalled: Boolean = false
            private set

        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> {
            sessionTodayCalledWith = deckId
            return cards
        }

        override suspend fun sessionTodayAllDecks(): List<StudyFlashcard> {
            allDecksCalled = true
            return cards
        }

        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> = emptyFlow()
    }

    private class FakeStatsRepo(
        private val firstReviewedInRange: Int = 0,
    ) : StudyStatsRepository {

        var firstReviewedStart: Instant? = null
            private set

        var firstReviewedEndExclusive: Instant? = null
            private set

        override suspend fun countDistinctCardsStudiedToday(): Int = 0

        override suspend fun countReviewsDue(now: Instant): Int = 0

        override suspend fun countNewCards(): Int = 0

        override suspend fun countCardsFirstReviewedIn(start: Instant, endExclusive: Instant): Int {
            firstReviewedStart = start
            firstReviewedEndExclusive = endExclusive
            return firstReviewedInRange
        }

        override suspend fun countCardsDueThisWeek(): Int = 0

        override suspend fun countCardsDueInRange(startMillis: Long, endMillis: Long): Int = 0

        override suspend fun findNextReviewAtAfter(millis: Long): Long? = null

        override suspend fun findReviewTimestampsDescending(): List<Long> = emptyList()
    }
}
