package com.emm.hello.di

import com.emm.data.scrap.OxfordScrapper
import com.emm.data.scrap.ScrapWordContentRepository
import com.emm.domain.WordContentCreator
import com.emm.domain.WordContentFetcher
import com.emm.domain.WordContentRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val casesModule = module {

    factory {
        ScrapWordContentRepository(
            oxfordScrapper = OxfordScrapper(),
            wordContentDao = get(),
            exampleDao = get(),
        )
    } bind WordContentRepository::class
    factoryOf(::WordContentFetcher)
    factoryOf(::WordContentCreator)
}