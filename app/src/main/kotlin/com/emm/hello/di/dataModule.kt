package com.emm.hello.di

import com.emm.data.AppDatabase
import com.emm.data.WordDao
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    single<WordDao> {
        AppDatabase.create(androidApplication()).wordDao()
    }
}