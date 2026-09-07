package com.emm.hello.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher

fun ManagedActivityResultLauncher<String, Boolean>.requestPostNotificationsPermission(onUnavailable: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        onUnavailable()
    }
}
