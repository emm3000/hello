package com.emm.hello.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emm.domain.dailywords.PrepareScheduledDailyWordsUseCase
import com.emm.domain.suggestion.WordSuggestions
import com.emm.hello.MainActivity
import com.emm.hello.R
import com.emm.hello.core.theme.ink
import com.emm.hello.navigation.LaunchDestination
import org.koin.core.context.GlobalContext

class DailyWordsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.canShowDailyWords()) return Result.success()

        val prepareScheduledDailyWords: PrepareScheduledDailyWordsUseCase =
            GlobalContext.get().get<PrepareScheduledDailyWordsUseCase>()
        val batch: WordSuggestions = prepareScheduledDailyWords() ?: return Result.success()

        ensureDailyWordsChannel(applicationContext)
        post(notificationManager, buildNotification(batch))
        return Result.success()
    }

    private fun NotificationManagerCompat.canShowDailyWords(): Boolean {
        if (!areNotificationsEnabled()) return false
        val channel: NotificationChannel? = getNotificationChannel(DAILY_WORDS_CHANNEL_ID)
        return channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun buildNotification(batch: WordSuggestions): Notification {
        val context: Context = applicationContext
        val count: Int = batch.words.size
        val body: String = context.resources.getQuantityString(
            R.plurals.notification_daily_words_body,
            count,
            batch.situation,
            count,
        )

        return NotificationCompat.Builder(context, DAILY_WORDS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ink.toArgb())
            .setColorized(false)
            .setContentTitle(context.getString(R.string.notification_daily_words_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(buildContentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun buildContentIntent(context: Context): PendingIntent {
        val intent: Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LaunchDestination.EXTRA_NAME, LaunchDestination.DailyWords.extraValue)
        }
        return PendingIntent.getActivity(
            context,
            DAILY_WORDS_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun post(notificationManager: NotificationManagerCompat, notification: Notification) {
        try {
            notificationManager.notify(DAILY_WORDS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            return
        }
    }
}
