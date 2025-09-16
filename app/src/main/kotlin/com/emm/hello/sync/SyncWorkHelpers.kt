package com.emm.hello.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import com.emm.hello.R

val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresCharging(true)
        .build()

fun Context.syncForegroundInfo(channelName: String, notificationId: Int, channelId: String) = ForegroundInfo(
    notificationId,
    syncWorkNotification(channelName, channelId),
)

private fun Context.syncWorkNotification(channelName: String, channelId: String): Notification {
    val channel = NotificationChannel(
        /* id = */ channelId,
        /* name = */ channelName,
        /* importance = */ NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Notificación para sincronización"
    }
    val notificationManager: NotificationManager? = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    notificationManager?.createNotificationChannel(channel)

    return NotificationCompat.Builder(
        this,
        channelId,
    )
        .setSmallIcon(R.drawable.outline_save_24)
        .setContentTitle("Estamos sincronizando")
        .setContentText("Esta es una frase de prueba para validar la notificación.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}