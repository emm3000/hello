package com.emm.data.study

import com.emm.data.remote.DataStore
import com.emm.domain.study.DailyNewCardLimit
import com.emm.domain.study.DailyNewCardLimitRepository

class DataStoreDailyNewCardLimitRepository(
    private val dataStore: DataStore,
) : DailyNewCardLimitRepository {

    override fun get(): DailyNewCardLimit =
        DailyNewCardLimit.fromCards(dataStore.dailyNewCardLimit) ?: DailyNewCardLimit.DEFAULT

    override fun set(limit: DailyNewCardLimit) {
        dataStore.dailyNewCardLimit = limit.cards
    }
}
