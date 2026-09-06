package com.emm.domain.reminder

import java.time.LocalTime

interface StudyReminderScheduler {

    fun schedule(time: LocalTime)

    fun cancel()
}
