package com.emm.data.flashcard

interface GeminiTelemetry {

    fun recordCallFailure(kind: String, attempts: Int, cause: Throwable)

    fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable)

    object NoOp : GeminiTelemetry {
        override fun recordCallFailure(kind: String, attempts: Int, cause: Throwable) = Unit
        override fun recordParseFailure(kind: String, rawResponse: String, cause: Throwable) = Unit
    }
}
