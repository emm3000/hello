package com.emm.hello.analytics

import android.util.Log

class LogcatProductAnalytics : ProductAnalytics {

    override fun track(event: ProductEvent) {
        val payload: AnalyticsPayload = event.toPayload()
        Log.i(TAG, "${payload.name} ${payload.params}")
    }
}

private const val TAG: String = "ProductAnalytics"
