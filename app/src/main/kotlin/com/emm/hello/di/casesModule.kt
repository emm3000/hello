package com.emm.hello.di

import android.content.Context
import com.emm.data.wordcontent.OxfordScrapper
import com.emm.data.wordcontent.DefaultWordContentRepository
import com.emm.data.wordcontent.GeminiService
import com.emm.domain.WordContentCreator
import com.emm.domain.WordContentFetcher
import com.emm.domain.WordContentRepository
import com.emm.data.R
import com.google.ai.client.generativeai.GenerativeModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val casesModule = module {

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
    factoryOf(::WordContentFetcher)
    factoryOf(::WordContentCreator)
}

private fun provideGenerativeModel(context: Context) = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = context.getString(R.string.xmm),
)