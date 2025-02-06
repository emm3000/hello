package com.emm.hello.di

import com.emm.data.LocalWordRepository
import com.emm.hello.features.backup.domain.LocalStorageRepository
import com.emm.hello.features.backup.domain.SharedLocalStorageRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::LocalWordRepository) bind com.emm.domain.WordRepository::class
    factoryOf(::SharedLocalStorageRepository) bind LocalStorageRepository::class
}