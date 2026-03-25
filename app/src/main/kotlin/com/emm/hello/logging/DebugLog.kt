package com.emm.hello.logging

import android.util.Log

internal fun logInfo(tag: String, message: String) {
    runCatching { Log.i(tag, message) }.getOrElse { println("I/$tag: $message") }
}

internal fun logWarn(tag: String, message: String, error: Throwable? = null) {
    runCatching {
        if (error != null) Log.w(tag, message, error) else Log.w(tag, message)
    }.getOrElse {
        println("W/$tag: $message")
        error?.printStackTrace()
    }
}

internal fun logError(tag: String, message: String, error: Throwable? = null) {
    runCatching {
        if (error != null) Log.e(tag, message, error) else Log.e(tag, message)
    }.getOrElse {
        println("E/$tag: $message")
        error?.printStackTrace()
    }
}
