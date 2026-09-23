package com.emm.domain.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NewCardBudgetTest {

    @Test
    fun `a fresh day allows the full daily limit`() {
        val budget = NewCardBudget(introducedToday = 0, dailyLimit = 10)

        assertEquals(DailyNewCardLimit.DEFAULT.cards, budget.remaining)
        assertEquals(DailyNewCardLimit.DEFAULT.cards, budget.allow(available = 50))
    }

    @Test
    fun `cards already introduced today shrink what is left`() {
        val budget = NewCardBudget(introducedToday = 4, dailyLimit = 10)

        assertEquals(6, budget.remaining)
        assertEquals(6, budget.allow(available = 25))
    }

    @Test
    fun `a spent budget allows nothing`() {
        val budget = NewCardBudget(introducedToday = DailyNewCardLimit.DEFAULT.cards, dailyLimit = 10)

        assertEquals(0, budget.remaining)
        assertEquals(0, budget.allow(available = 25))
    }

    @Test
    fun `overshooting the limit never yields a negative remainder`() {
        val budget = NewCardBudget(introducedToday = 17, dailyLimit = 10)

        assertEquals(0, budget.remaining)
        assertEquals(0, budget.allow(available = 25))
    }

    @Test
    fun `fewer available cards than remaining budget caps the answer at what exists`() {
        val budget = NewCardBudget(introducedToday = 0, dailyLimit = 10)

        assertEquals(2, budget.allow(available = 2))
    }

    @Test
    fun `a negative availability is treated as nothing available`() {
        val budget = NewCardBudget(introducedToday = 0, dailyLimit = 10)

        assertEquals(0, budget.allow(available = -3))
    }

    @Test
    fun `a custom daily limit replaces the default`() {
        val budget = NewCardBudget(introducedToday = 1, dailyLimit = 3)

        assertEquals(2, budget.remaining)
        assertEquals(2, budget.allow(available = 9))
    }

    @Test
    fun `a negative introduced count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { NewCardBudget(introducedToday = -1, dailyLimit = 10) }
    }

    @Test
    fun `a negative daily limit is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewCardBudget(introducedToday = 0, dailyLimit = -1)
        }
    }

    @Test
    fun `extending a spent budget allows exactly the extra`() {
        val budget: NewCardBudget = NewCardBudget(introducedToday = DailyNewCardLimit.DEFAULT.cards, dailyLimit = 10)
            .extendedBy(EXTRA_NEW_CARDS_PER_REQUEST)

        assertEquals(EXTRA_NEW_CARDS_PER_REQUEST, budget.remaining)
        assertEquals(EXTRA_NEW_CARDS_PER_REQUEST, budget.allow(available = 50))
    }

    @Test
    fun `extending an untouched budget still allows exactly the extra`() {
        val budget: NewCardBudget = NewCardBudget(introducedToday = 0, dailyLimit = 10).extendedBy(4)

        assertEquals(4, budget.remaining)
        assertEquals(4, budget.allow(available = 50))
    }

    @Test
    fun `extending an overspent budget allows exactly the extra`() {
        val budget: NewCardBudget = NewCardBudget(introducedToday = 17, dailyLimit = 10).extendedBy(3)

        assertEquals(3, budget.remaining)
        assertEquals(3, budget.allow(available = 50))
    }

    @Test
    fun `extending by nothing allows nothing`() {
        val budget: NewCardBudget = NewCardBudget(introducedToday = 2, dailyLimit = 10).extendedBy(0)

        assertEquals(0, budget.remaining)
        assertEquals(0, budget.allow(available = 50))
    }

    @Test
    fun `a negative extension is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewCardBudget(introducedToday = 0, dailyLimit = 10).extendedBy(-1)
        }
    }
}
