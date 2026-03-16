package com.emm.data.sync

import org.koin.dsl.module

val syncDataModule = module {
    single { SupabaseSyncRemoteDataSource(get(), get()) }
}
