package com.emm.hello.di

import com.emm.data.flashcard.GeminiService
import com.emm.data.flashcard.LearningNoteResponseSchema
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import org.koin.dsl.module

private const val MODEL_NAME = "gemini-2.5-flash-lite"
private const val DEFAULT_TEMPERATURE = 0.1f
private const val DEFAULT_TOP_P = 0.95f

/**
 * Provee dependencias de infraestructura compartidas.
 * GeminiService es usado por DefaultFlashcardRepository.
 * - `generativeModel`: usado por las regeneraciones parciales (shape variable).
 * - `learningNoteModel`: aplica `responseSchema` para la generación principal.
 */
val repositoryModule = module {
    single {
        GeminiService(
            generativeModel = provideGenericModel(),
            learningNoteModel = provideLearningNoteModel(),
        )
    }
}

private fun provideGenericModel(): GenerativeModel = Firebase.ai(
    backend = GenerativeBackend.googleAI()
).generativeModel(
    modelName = MODEL_NAME,
    generationConfig = generationConfig {
        responseMimeType = "application/json"
        temperature = DEFAULT_TEMPERATURE
        topP = DEFAULT_TOP_P
    }
)

private fun provideLearningNoteModel(): GenerativeModel = Firebase.ai(
    backend = GenerativeBackend.googleAI()
).generativeModel(
    modelName = MODEL_NAME,
    generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = LearningNoteResponseSchema.schema
        temperature = DEFAULT_TEMPERATURE
        topP = DEFAULT_TOP_P
    }
)
