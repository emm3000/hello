package com.emm.domain.study

const val DEFAULT_DAILY_NEW_CARD_LIMIT: Int = 10

data class NewCardBudget(
    val introducedToday: Int,
    val dailyLimit: Int = DEFAULT_DAILY_NEW_CARD_LIMIT,
) {

    init {
        require(introducedToday >= 0) { "introducedToday must be non-negative, was $introducedToday." }
        require(dailyLimit >= 0) { "dailyLimit must be non-negative, was $dailyLimit." }
    }

    val remaining: Int = (dailyLimit - introducedToday).coerceAtLeast(0)

    fun allow(available: Int): Int = minOf(available.coerceAtLeast(0), remaining)
}
