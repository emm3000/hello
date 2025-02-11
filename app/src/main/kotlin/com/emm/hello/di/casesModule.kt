package com.emm.hello.di

import com.emm.domain.anki.AnkiCreator
import com.emm.domain.word.WordContentCreator
import com.emm.domain.word.WordContentFetcher
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val casesModule = module {
    factoryOf(::WordContentFetcher)
    factoryOf(::WordContentCreator)

    factoryOf(::AnkiCreator)
}

