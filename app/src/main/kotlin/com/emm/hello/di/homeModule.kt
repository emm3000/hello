package com.emm.hello.di

import com.emm.hello.features.anki.AnkiViewModel
import com.emm.hello.features.backup.DataModeler
import com.emm.hello.features.home.HomeViewModel
import com.emm.hello.features.backup.JustFiles
import com.emm.hello.features.detail.DetailViewModel
import com.emm.hello.features.main.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::AnkiViewModel)

    factory { DataModeler(get(), get(), get()) }
    factory {
        JustFiles(
            dataModeler = get(),
            context = androidApplication()
        )
    }
}