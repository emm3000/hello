package com.emm.hello.telemetry

import com.emm.domain.telemetry.GeminiTelemetry
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsGeminiTelemetry(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : GeminiTelemetry {

    override fun recordCallFailure(kind: String, attempts: Int, cause: Throwable) {
        crashlytics.setCustomKey("gemini_call_kind", kind)
        crashlytics.setCustomKey("gemini_call_attempts", attempts)
        crashlytics.log("Gemini call failed after $attempts attempts (kind=$kind)")
        crashlytics.recordException(cause)
    }

    override fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable) {
        crashlytics.setCustomKey("gemini_parse_kind", kind)
        crashlytics.setCustomKey("gemini_parse_raw_truncated", rawResponse)
        crashlytics.log("Gemini parse failed (kind=$kind)")
        crashlytics.recordException(cause)
    }

    override fun recordQuotaExceeded(kind: String, limit: Int) {
        crashlytics.setCustomKey("gemini_quota_kind", kind)
        crashlytics.setCustomKey("gemini_quota_limit", limit)
        crashlytics.log("Gemini daily quota exceeded (kind=$kind, limit=$limit)")
    }
}
