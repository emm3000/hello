package com.emm.data.flashcard

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse

open class GeminiService(
    private val generativeModel: GenerativeModel,
    private val learningNoteModel: GenerativeModel = generativeModel,
) {

    open suspend fun process(prompt: String): String {
        val generateContent: GenerateContentResponse = generativeModel.generateContent(prompt)
        val response: String = generateContent.text.orEmpty()
        return response
    }

    /**
     * Llamada dedicada a la generación principal de [com.emm.data.flashcard.iadto.GeneratedLearningNoteDto].
     * Usa un modelo con `responseSchema` declarado para restringir enums y forma del JSON.
     */
    open suspend fun processLearningNote(prompt: String): String {
        val generateContent: GenerateContentResponse = learningNoteModel.generateContent(prompt)
        return generateContent.text.orEmpty()
    }
}
