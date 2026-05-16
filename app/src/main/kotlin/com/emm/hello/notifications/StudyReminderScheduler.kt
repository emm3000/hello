package com.emm.hello.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object StudyReminderScheduler {

    private const val UNIQUE_WORK_NAME = "study_reminder_daily"
    private const val REPEAT_INTERVAL_HOURS = 24L
    private const val FLEX_INTERVAL_HOURS = 1L
    private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(19, 0)

    fun scheduleDaily(context: Context) {
        val initialDelayMinutes = minutesUntilNext(DEFAULT_REMINDER_TIME)
        val request = PeriodicWorkRequestBuilder<DueCardsReminderWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_HOURS,
            flexTimeIntervalUnit = TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun minutesUntilNext(target: LocalTime): Long {
        val now = ZonedDateTime.now()
        var firingTime = now.with(target)
        if (!firingTime.isAfter(now)) {
            firingTime = firingTime.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, firingTime).coerceAtLeast(0)
    }
}
