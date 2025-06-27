package com.emm.hello.di

import android.content.Context
import com.emm.data.R
import com.emm.data.deprecated.anki.DefaultAnkiRepository
import com.emm.data.deprecated.word.LocalWordRepository
import com.emm.data.deprecated.wordcontent.DefaultWordContentRepository
import com.emm.data.deprecated.wordcontent.OxfordScrapper
import com.emm.data.flashcard.GeminiService
import com.emm.domain.deprecated.anki.AnkiRepository
import com.emm.domain.deprecated.word.WordContentRepository
import com.emm.domain.deprecated.word.WordRepository
import com.emm.hello.features.backup.domain.LocalStorageRepository
import com.emm.hello.features.backup.domain.SharedLocalStorageRepository
import com.google.ai.client.generativeai.GenerativeModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::LocalWordRepository) bind WordRepository::class
    factoryOf(::SharedLocalStorageRepository) bind LocalStorageRepository::class
    single {
        GeminiService(
            generativeModel = provideGenerativeModel(androidApplication())
        )
    }
    factory {
        DefaultWordContentRepository(
            oxfordScrapper = OxfordScrapper(),
            geminiService = get(),
            wordContentDao = get(),
            exampleDao = get(),
        )
    } bind WordContentRepository::class
    factory {
        DefaultAnkiRepository(
            get()
        )
    } bind AnkiRepository::class
}

private fun provideGenerativeModel(context: Context) = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = context.getString(R.string.xmm),
)