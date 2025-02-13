package com.emm.hello

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

fun Context.hasPermissions(): Boolean {
    return isSdk30OrNewer(
        truly = ::checkExternalStorage,
        falsely = ::checkPermissionAndroidLegacy,
    )
}

fun <T> isSdk30OrNewer(
    truly: () -> T,
    falsely: () -> T,
): T = if (isApi30OrHigher()) truly() else falsely()

private fun checkExternalStorage(): Boolean {
    val isAtLeastR: Boolean = isApi30OrHigher()
    return if (isAtLeastR) Environment.isExternalStorageManager() else false
}

private fun isApi30OrHigher() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

private fun Context.checkPermissionAndroidLegacy(): Boolean {
    return listOf(
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    ).all { it }
}

private fun Context.checkPermission(permission: String): Boolean {
    val permissionGranted: Int = PackageManager.PERMISSION_GRANTED
    return ContextCompat.checkSelfPermission(this, permission) == permissionGranted
}