package com.emm.hello.newfeatures.today

import com.emm.domain.study.DashboardStats
import com.emm.domain.study.EXTRA_NEW_CARDS_PER_REQUEST
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayUiStateTest {

    @Test
    fun `ring is empty when nothing was studied and nothing is due`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 0,
                cardsDueToday = 0,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(0f, state.ringProgress, 0.0001f)
    }

    @Test
    fun `ring is empty before the first review of the day`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 0,
                cardsDueToday = 8,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(0f, state.ringProgress, 0.0001f)
    }

    @Test
    fun `ring fills by the share of today's cards already reviewed`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 3,
                cardsDueToday = 5,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(0.375f, state.ringProgress, 0.0001f)
    }

    @Test
    fun `ring is full when everything due was reviewed`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 8,
                cardsDueToday = 0,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(1f, state.ringProgress, 0.0001f)
    }

    @Test
    fun `ring is empty while stats are loading`() {
        val state = TodayUiState()

        assertEquals(0f, state.ringProgress, 0.0001f)
    }

    @Test
    fun `day number is one while stats are loading`() {
        val state = TodayUiState()

        assertEquals(1, state.dayNumber)
    }

    @Test
    fun `day number is the next day of a streak that has not been studied today`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 0,
                cardsDueToday = 0,
                currentStreak = 5,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(6, state.dayNumber)
    }

    @Test
    fun `day number is the streak once today was studied`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 4,
                cardsDueToday = 0,
                currentStreak = 6,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(6, state.dayNumber)
    }

    @Test
    fun `day number is one on the very first day`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 0,
                cardsDueToday = 0,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(1, state.dayNumber)
    }

    @Test
    fun `more new cards never offers beyond one batch`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 10,
                cardsDueToday = 0,
                currentStreak = 3,
                cardsDueThisWeek = 0,
                heldBackNewCards = 25,
            ),
        )

        assertEquals(EXTRA_NEW_CARDS_PER_REQUEST, state.moreNewCards)
    }

    @Test
    fun `more new cards offers exactly what is held back below a full batch`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 10,
                cardsDueToday = 0,
                currentStreak = 3,
                cardsDueThisWeek = 0,
                heldBackNewCards = 3,
            ),
        )

        assertEquals(3, state.moreNewCards)
    }

    @Test
    fun `more new cards offers nothing while stats are loading`() {
        val state = TodayUiState()

        assertEquals(0, state.moreNewCards)
    }

    @Test
    fun `more new cards offers nothing when nothing was held back`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 2,
                cardsDueToday = 4,
                currentStreak = 1,
                cardsDueThisWeek = 4,
            ),
        )

        assertEquals(0, state.moreNewCards)
    }

    @Test
    fun `day number never drops below one after studying`() {
        val state = TodayUiState(
            stats = DashboardStats(
                cardsStudiedToday = 2,
                cardsDueToday = 0,
                currentStreak = 0,
                cardsDueThisWeek = 0,
            ),
        )

        assertEquals(1, state.dayNumber)
    }
}
