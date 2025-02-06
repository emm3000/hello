package com.emm.hello.di

import com.emm.data.AppDatabase
import com.emm.data.scrap.ExampleDao
import com.emm.data.scrap.WordContentDao
import com.emm.data.word.WordDao
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    single<AppDatabase> {
        AppDatabase.create(androidApplication())
    }
    single<WordDao> {
        get<AppDatabase>().wordDao()
    }
    single<ExampleDao> {
        get<AppDatabase>().exampleDao()
    }
    single<WordContentDao> {
        get<AppDatabase>().wordContentDao()
    }
}