package com.emm.hello

import android.os.Build

fun <T> isAtLeastApi30(
    truly: () -> T,
    falsely: () -> T,
): T = if (requiresQ()) truly() else falsely()

private fun requiresQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

