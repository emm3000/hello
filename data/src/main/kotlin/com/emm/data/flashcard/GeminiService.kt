package com.emm.data.flashcard

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse

class GeminiService(private val generativeModel: GenerativeModel) {

    suspend fun process(prompt: String): String {
        val generateContent: GenerateContentResponse = generativeModel.generateContent(prompt)
        val response: String = generateContent.text.orEmpty()
        return cleanResponse(response)
    }

    private fun cleanResponse(response: String): String = response
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}
