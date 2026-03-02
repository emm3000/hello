package com.emm.hello.di

import android.content.Context
import com.emm.data.R
import com.emm.data.flashcard.GeminiService
import com.google.ai.client.generativeai.GenerativeModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/**
 * Provee dependencias de infraestructura compartidas.
 * GeminiService es usado por DefaultFlashcardRepository y DefaultQuoteRepository.
 */
val repositoryModule = module {
    single {
        GeminiService(
            generativeModel = provideGenerativeModel(androidApplication())
        )
    }
}

private fun provideGenerativeModel(context: Context) = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = context.getString(R.string.xmm),
)
