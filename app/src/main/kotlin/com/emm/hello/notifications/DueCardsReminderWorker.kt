package com.emm.hello.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emm.domain.flashcard.CountDueFlashcardsUseCase
import com.emm.hello.MainActivity
import com.emm.hello.R
import com.emm.hello.core.theme.ink
import org.koin.core.context.GlobalContext

class DueCardsReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val countDueFlashcards = GlobalContext.get().get<CountDueFlashcardsUseCase>()
        val dueCount = runCatching { countDueFlashcards() }.getOrNull() ?: return Result.retry()
        if (dueCount <= 0L) return Result.success()

        ensureStudyReminderChannel(applicationContext)
        postReminder(dueCount.toInt())
        return Result.success()
    }

    private fun postReminder(dueCount: Int) {
        val context = applicationContext
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = context.resources.getQuantityString(
            R.plurals.notification_reminder_body,
            dueCount,
            dueCount,
        )

        val notification = NotificationCompat.Builder(context, STUDY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ink.toArgb())
            .setColorized(false)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(STUDY_REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission was revoked between channel check and post; ignore.
        }
    }
}
