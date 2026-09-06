package com.emm.hello.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emm.domain.reminder.StudyReminderScheduler
import com.emm.domain.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "study_reminder_daily"
private const val REPEAT_INTERVAL_HOURS = 24L

class WorkManagerStudyReminderScheduler(
    private val context: Context,
    private val clock: Clock,
) : StudyReminderScheduler {

    override fun schedule(time: LocalTime) {
        val now: ZonedDateTime = clock.now().atZone(ZoneId.systemDefault())
        val nextRun: ZonedDateTime = nextOccurrence(time, now)
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<DueCardsReminderWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setNextScheduleTimeOverride(nextRun.toInstant().toEpochMilli())
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
}
