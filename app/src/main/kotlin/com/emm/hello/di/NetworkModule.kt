package com.emm.hello.di

import android.content.SharedPreferences
import com.emm.data.remote.provideSharedPreferences
import com.emm.data.remote.supabaseModule
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val networkModule = module {
    includes(supabaseModule)

    single<SharedPreferences> { provideSharedPreferences(androidApplication()) }
}
