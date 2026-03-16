package com.emm.hello.di

import com.emm.data.deck.RemoteDataSource
import com.emm.data.deck.RequestDataProcessor
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val syncModule = module {
    factoryOf(::RemoteDataSource)
    factoryOf(::RequestDataProcessor)
}
