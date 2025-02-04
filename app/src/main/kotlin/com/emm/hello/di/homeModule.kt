package com.emm.hello.di

import com.emm.hello.page.DataModeler
import com.emm.hello.page.HomeViewModel
import com.emm.hello.page.InitViewModel
import com.emm.hello.page.JustFiles
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