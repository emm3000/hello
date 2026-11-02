package com.emm.domain.dailywords

import java.time.LocalTime

interface DailyWordsScheduler {

    fun schedule(time: LocalTime)

    fun cancel()
}
