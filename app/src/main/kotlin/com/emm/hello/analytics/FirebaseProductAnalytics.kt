package com.emm.hello.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseProductAnalytics(
    private val firebaseAnalytics: FirebaseAnalytics,
) : ProductAnalytics {

    override fun track(event: ProductEvent) {
        val payload: AnalyticsPayload = event.toPayload()
        firebaseAnalytics.logEvent(payload.name, payload.params.toBundle())
    }
}

private fun Map<String, Any>.toBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, value) ->
        when (value) {
            is Long -> bundle.putLong(key, value)
            is String -> bundle.putString(key, value)
        }
    }
    return bundle
}
