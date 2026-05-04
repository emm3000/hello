package com.emm.hello.logging

import android.util.Log

internal fun logError(tag: String, message: String, error: Throwable? = null) {
    if (error != null) Log.e(tag, message, error) else Log.e(tag, message)
}
