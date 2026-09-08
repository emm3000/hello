package com.emm.domain.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NewCardBudgetTest {

    @Test
    fun `a fresh day allows the full daily limit`() {
        val budget = NewCardBudget(introducedToday = 0)

        assertEquals(DEFAULT_DAILY_NEW_CARD_LIMIT, budget.remaining)
        assertEquals(DEFAULT_DAILY_NEW_CARD_LIMIT, budget.allow(available = 50))
    }

    @Test
    fun `cards already introduced today shrink what is left`() {
        val budget = NewCardBudget(introducedToday = 4)

        assertEquals(6, budget.remaining)
        assertEquals(6, budget.allow(available = 25))
    }

    @Test
    fun `a spent budget allows nothing`() {
        val budget = NewCardBudget(introducedToday = DEFAULT_DAILY_NEW_CARD_LIMIT)

        assertEquals(0, budget.remaining)
        assertEquals(0, budget.allow(available = 25))
    }

    @Test
    fun `overshooting the limit never yields a negative remainder`() {
        val budget = NewCardBudget(introducedToday = 17)

        assertEquals(0, budget.remaining)
        assertEquals(0, budget.allow(available = 25))
    }

    @Test
    fun `fewer available cards than remaining budget caps the answer at what exists`() {
        val budget = NewCardBudget(introducedToday = 0)

        assertEquals(2, budget.allow(available = 2))
    }

    @Test
    fun `a negative availability is treated as nothing available`() {
        val budget = NewCardBudget(introducedToday = 0)

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
        assertThrows(IllegalArgumentException::class.java) { NewCardBudget(introducedToday = -1) }
    }

    @Test
    fun `a negative daily limit is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewCardBudget(introducedToday = 0, dailyLimit = -1)
        }
    }
}
