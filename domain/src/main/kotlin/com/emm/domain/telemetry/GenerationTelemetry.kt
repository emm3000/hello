package com.emm.domain.telemetry

interface GenerationTelemetry {

    fun recordCallFailure(kind: String, attempts: Int, cause: Throwable)

    fun recordParseFailure(kind: String, responseLength: Int, cause: Throwable)

    object NoOp : GenerationTelemetry {
        override fun recordCallFailure(kind: String, attempts: Int, cause: Throwable) = Unit
        override fun recordParseFailure(kind: String, responseLength: Int, cause: Throwable) = Unit
    }
}
