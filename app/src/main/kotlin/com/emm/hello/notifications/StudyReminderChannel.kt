package com.emm.hello.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.emm.hello.R

const val STUDY_REMINDER_CHANNEL_ID = "study_reminders"
const val STUDY_REMINDER_NOTIFICATION_ID = 1001

fun ensureStudyReminderChannel(context: Context) {
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(STUDY_REMINDER_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        STUDY_REMINDER_CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.notification_channel_description)
    }
    manager.createNotificationChannel(channel)
}
