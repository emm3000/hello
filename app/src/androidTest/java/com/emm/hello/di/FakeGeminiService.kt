package com.emm.hello.di

import com.emm.data.flashcard.GeminiService
import com.google.firebase.ai.GenerativeModel
import io.mockk.mockk

/**
 * No-op [GeminiService] for instrumented tests.
 *
 * Overrides [process] to return an empty string, preventing any network
 * calls to the Gemini API. The underlying [GenerativeModel] is a relaxed
 * mock so the super constructor is satisfied but never invoked.
 */
class FakeGeminiService : GeminiService(
    generativeModel = mockk(relaxed = true),
) {

    override suspend fun process(prompt: String): String = ""
}
