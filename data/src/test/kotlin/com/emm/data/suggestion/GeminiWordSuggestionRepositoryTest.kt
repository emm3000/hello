package com.emm.data.suggestion

import com.emm.data.flashcard.GeminiService
import com.google.firebase.ai.GenerativeModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiWordSuggestionRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `suggest calls Gemini exactly once and includes every recent word in the prompt`() = runTest {
        val canned = """
            {"situation": "Ordering food at a busy restaurant", "words": [{"word": "the check", "translation": "la cuenta"}]}
        """.trimIndent()
        val geminiService = RecordingGeminiService(response = canned)
        val repository = GeminiWordSuggestionRepository(
            geminiService = geminiService,
            json = json,
            ioDispatcher = Dispatchers.IO,
        )
        val recentWords = listOf("hello", "goodbye", "please")

        val result = repository.suggest(recentWords)

        assertEquals(1, geminiService.callCount)
        recentWords.forEach { word -> assertTrue(geminiService.lastPrompt.contains(word)) }
        assertEquals("Ordering food at a busy restaurant", result.situation)
        assertEquals(1, result.words.size)
    }
}

private class RecordingGeminiService(
    private val response: String,
) : GeminiService(generativeModel = mockk<GenerativeModel>()) {

    var callCount: Int = 0
        private set
    var lastPrompt: String = ""
        private set

    override suspend fun process(prompt: String): String {
        callCount += 1
        lastPrompt = prompt
        return response
    }
}
