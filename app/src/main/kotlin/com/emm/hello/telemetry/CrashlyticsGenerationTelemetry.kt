package com.emm.hello.telemetry

import com.emm.domain.telemetry.GenerationTelemetry
import com.emm.hello.logging.logError
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsGenerationTelemetry(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : GenerationTelemetry {

    private companion object {
        const val TAG: String = "GenerationTelemetry"
    }

    override fun recordCallFailure(kind: String, attempts: Int, cause: Throwable) {
        crashlytics.setCustomKey("generation_call_kind", kind)
        crashlytics.setCustomKey("generation_call_attempts", attempts)
        crashlytics.log("Generation call failed after $attempts attempts (kind=$kind)")
        crashlytics.recordException(cause)
    }

    override fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable) {
        crashlytics.setCustomKey("generation_parse_kind", kind)
        crashlytics.setCustomKey("generation_parse_raw_truncated", rawResponse)
        crashlytics.log("Generation parse failed (kind=$kind)")
        crashlytics.recordException(cause)
        logError(TAG, "generation:parse-failed kind=$kind raw=$rawResponse", cause)
    }
}
