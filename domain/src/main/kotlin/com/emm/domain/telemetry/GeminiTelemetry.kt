package com.emm.domain.telemetry

interface GeminiTelemetry {

    fun recordCallFailure(kind: String, attempts: Int, cause: Throwable)

    fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable)

    fun recordQuotaExceeded(kind: String, limit: Int)

    object NoOp : GeminiTelemetry {
        override fun recordCallFailure(kind: String, attempts: Int, cause: Throwable) = Unit
        override fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable) = Unit
        override fun recordQuotaExceeded(kind: String, limit: Int) = Unit
    }
}
