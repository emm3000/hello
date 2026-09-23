package com.emm.domain.study

interface DailyNewCardLimitRepository {
    fun get(): DailyNewCardLimit
    fun set(limit: DailyNewCardLimit)
}
