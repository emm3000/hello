package com.emm.data.wordcontent

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(private val generativeModel: GenerativeModel) {

    suspend fun process(word: String): String = withContext(Dispatchers.IO) {
        val prompt = "Eres un profesor experto de inglés dame que significa y cuando usar esta palabra o frase \"${word}\""
        val generateContent: GenerateContentResponse = generativeModel.generateContent(prompt)
        generateContent.text.orEmpty()
    }
}