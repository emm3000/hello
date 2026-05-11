package com.emm.data.flashcard

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse

open class GeminiService(private val generativeModel: GenerativeModel) {

    open suspend fun process(prompt: String): String {
        val generateContent: GenerateContentResponse = generativeModel.generateContent(prompt)
        val response: String = generateContent.text.orEmpty()
        return response
    }
}
