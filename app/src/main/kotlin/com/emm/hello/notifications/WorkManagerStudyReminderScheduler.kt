package com.emm.hello.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emm.domain.reminder.StudyReminderScheduler
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "study_reminder_daily"
private const val REPEAT_INTERVAL_HOURS = 24L
private const val FLEX_INTERVAL_HOURS = 1L

class WorkManagerStudyReminderScheduler(
    private val context: Context,
) : StudyReminderScheduler {

    override fun schedule(time: LocalTime) {
        val initialDelayMinutes: Long = minutesUntilNext(time)
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<DueCardsReminderWorker>(
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

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun minutesUntilNext(target: LocalTime): Long {
        val now: ZonedDateTime = ZonedDateTime.now()
        var firingTime: ZonedDateTime = now.with(target)
        if (!firingTime.isAfter(now)) {
            firingTime = firingTime.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, firingTime).coerceAtLeast(0)
    }
}
