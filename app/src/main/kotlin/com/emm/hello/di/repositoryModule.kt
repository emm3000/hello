package com.emm.hello.di
 
import com.emm.data.flashcard.GeminiService
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import org.koin.dsl.module

/**
 * Provee dependencias de infraestructura compartidas.
 * GeminiService es usado por DefaultFlashcardRepository y DefaultQuoteRepository.
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
    modelName = "gemini-3-flash-preview"
)
