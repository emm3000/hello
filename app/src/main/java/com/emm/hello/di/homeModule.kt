package com.emm.hello.di

import com.emm.hello.page.InitViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::InitViewModel)
}