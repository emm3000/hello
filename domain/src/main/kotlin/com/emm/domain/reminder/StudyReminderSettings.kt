package com.emm.domain.reminder

import java.time.LocalTime

data class StudyReminderSettings(
    val isEnabled: Boolean,
    val time: LocalTime,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(19, 0)
    }
}
