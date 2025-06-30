package com.emm.hello.di

import android.content.SharedPreferences
import com.emm.data.remote.BackupApi
import com.emm.data.remote.provideOkHttp
import com.emm.data.remote.provideRetrofit
import com.emm.data.remote.provideSharedPreferences
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {

    single<OkHttpClient> { provideOkHttp(androidApplication()) }
    single<Retrofit> { provideRetrofit(get(), get()) }
    single<BackupApi> { provideApi(get()) }
    single<SharedPreferences> { provideSharedPreferences(androidApplication()) }
}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}