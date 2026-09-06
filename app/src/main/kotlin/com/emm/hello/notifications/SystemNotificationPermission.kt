package com.emm.hello.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat

class SystemNotificationPermission(private val context: Context) : NotificationPermission {
    override fun isGranted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
