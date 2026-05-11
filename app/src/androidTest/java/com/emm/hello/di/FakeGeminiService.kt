package com.emm.hello.di

import com.emm.data.flashcard.GeminiService
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig

private const val DEFAULT_TEMPERATURE = 0.1f
private const val DEFAULT_TOP_P = 0.95f

/**
 * No-op [GeminiService] for instrumented tests.
 *
 * Overrides [process] to return an empty string, preventing any network
 * calls to the Gemini API. The underlying [GenerativeModel] is constructed
 * via the same Firebase path as production so the super constructor is
 * satisfied, but it is never invoked.
 */
open class FakeGeminiService : GeminiService(
    generativeModel = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
    ).generativeModel(
        modelName = "gemini-2.5-flash-lite",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = DEFAULT_TEMPERATURE
            topP = DEFAULT_TOP_P
        },
    ),
) {

    override suspend fun process(prompt: String): String = ""
}
