package com.emm.hello.di

import com.emm.data.deprecated.AppDatabase
import com.emm.data.deprecated.word.WordDao
import com.emm.data.deprecated.wordcontent.ExampleDao
import com.emm.data.deprecated.wordcontent.WordContentDao
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