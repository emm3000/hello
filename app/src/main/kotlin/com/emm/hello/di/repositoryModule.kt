package com.emm.hello.di

import android.content.Context
import com.emm.data.R
import com.emm.data.anki.DefaultAnkiRepository
import com.emm.data.word.LocalWordRepository
import com.emm.data.wordcontent.DefaultWordContentRepository
import com.emm.data.wordcontent.GeminiService
import com.emm.data.wordcontent.OxfordScrapper
import com.emm.domain.anki.AnkiRepository
import com.emm.domain.word.WordContentRepository
import com.emm.domain.word.WordRepository
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
    factory {
        DefaultWordContentRepository(
            oxfordScrapper = OxfordScrapper(),
            geminiService = GeminiService(
                generativeModel = provideGenerativeModel(androidApplication())
            ),
            wordContentDao = get(),
            exampleDao = get(),
        )
    } bind WordContentRepository::class
    factory {
        DefaultAnkiRepository(
            geminiService = GeminiService(
                generativeModel = provideGenerativeModel(androidApplication())
            )
        )
    } bind AnkiRepository::class
}

private fun provideGenerativeModel(context: Context) = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = context.getString(R.string.xmm),
)