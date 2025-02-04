package com.emm.hello.di

import com.emm.hello.features.backup.DataModeler
import com.emm.hello.features.home.HomeViewModel
import com.emm.hello.features.init.InitViewModel
import com.emm.hello.features.backup.JustFiles
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::InitViewModel)
    viewModelOf(::HomeViewModel)

    factory {
        JustFiles(
            dataModeler = DataModeler(get()),
            context = androidApplication()
        )
    }
}