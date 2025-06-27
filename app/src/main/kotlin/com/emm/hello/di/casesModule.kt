package com.emm.hello.di

import com.emm.domain.deprecated.anki.AnkiCreator
import com.emm.domain.deprecated.word.WordContentCreator
import com.emm.domain.deprecated.word.WordContentFetcher
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val casesModule = module {
    factoryOf(::WordContentFetcher)
    factoryOf(::WordContentCreator)

    factoryOf(::AnkiCreator)
}

