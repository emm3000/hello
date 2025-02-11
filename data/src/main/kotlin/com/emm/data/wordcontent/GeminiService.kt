package com.emm.data.wordcontent

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(private val generativeModel: GenerativeModel) {

    suspend fun process(prompt: String): String = withContext(Dispatchers.IO) {
        val generateContent: GenerateContentResponse = generativeModel.generateContent(prompt)
        generateContent.text.orEmpty()
    }
}