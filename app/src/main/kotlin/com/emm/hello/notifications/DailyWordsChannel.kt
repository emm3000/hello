package com.emm.hello.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.emm.hello.R

const val DAILY_WORDS_CHANNEL_ID = "daily_words"
const val DAILY_WORDS_NOTIFICATION_ID = 1002

fun ensureDailyWordsChannel(context: Context) {
    val manager: NotificationManager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(DAILY_WORDS_CHANNEL_ID) != null) return
    val channel: NotificationChannel = NotificationChannel(
        DAILY_WORDS_CHANNEL_ID,
        context.getString(R.string.notification_daily_words_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.notification_daily_words_channel_description)
    }
    manager.createNotificationChannel(channel)
}
