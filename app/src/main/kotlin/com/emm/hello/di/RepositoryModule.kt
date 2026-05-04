package com.emm.hello.di

import com.emm.data.flashcard.GeminiService
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import org.koin.dsl.module

private const val DEFAULT_TEMPERATURE = 0.1f
private const val DEFAULT_TOP_P = 0.95f

/**
 * Provee dependencias de infraestructura compartidas.
 * GeminiService es usado por DefaultFlashcardRepository.
 */
val repositoryModule = module {
    single {
        GeminiService(
            generativeModel = provideGenerativeModel()
        )
    }
}

private fun provideGenerativeModel(): GenerativeModel = Firebase.ai(
    backend = GenerativeBackend.googleAI()
).generativeModel(
    modelName = "gemini-2.5-flash-lite",
    generationConfig = generationConfig {
        responseMimeType = "application/json"
        temperature = DEFAULT_TEMPERATURE
        topP = DEFAULT_TOP_P
    }
)
