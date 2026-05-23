package com.emm.hello.di

import com.emm.data.flashcard.DailyGenerationQuota
import com.emm.data.flashcard.GeminiService
import com.emm.data.flashcard.GeminiTelemetry
import com.emm.data.flashcard.LearningNoteResponseSchema
import com.emm.domain.generation.GenerationQuota
import com.emm.hello.telemetry.CrashlyticsGeminiTelemetry
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import org.koin.dsl.module

private const val MODEL_NAME = "gemini-2.5-flash-lite"
private const val DEFAULT_TEMPERATURE = 0f
private const val DEFAULT_TOP_P = 0.95f

/**
 * Provides shared infrastructure dependencies.
 * GeminiService is used by DefaultFlashcardRepository.
 * - `generativeModel`: used for partial regenerations (variable shape).
 * - `learningNoteModel`: applies `responseSchema` for the main generation call.
 */
val repositoryModule = module {
    single<GeminiTelemetry> { CrashlyticsGeminiTelemetry() }
    single<GenerationQuota> { DailyGenerationQuota(preferences = get()) }
    single {
        GeminiService(
            generativeModel = provideGenericModel(),
            learningNoteModel = provideLearningNoteModel(),
            telemetry = get(),
            quota = get(),
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
