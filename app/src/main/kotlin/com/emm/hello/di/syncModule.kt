package com.emm.hello.di

import com.emm.data.deck.DeckSynchronizer
import com.emm.data.deck.RemoteDataSource
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val syncModule = module {
    factoryOf(::DeckSynchronizer)
    factoryOf(::RemoteDataSource)
}