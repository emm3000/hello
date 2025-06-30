package com.emm.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.emm.data.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

fun provideOkHttp(context: Context): OkHttpClient {

    val level: HttpLoggingInterceptor.Level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else HttpLoggingInterceptor.Level.NONE

    val loggingInterceptor = HttpLoggingInterceptor().apply {
        this.level = level
    }

    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(ChuckerInterceptor(context))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
}

fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
    val contentType = "application/json".toMediaType()

    return Retrofit.Builder()
        .client(client)
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
}

fun provideSharedPreferences(applicationContext: Context): SharedPreferences {
    return applicationContext.getSharedPreferences(BuildConfig.LIBRARY_PACKAGE_NAME, Context.MODE_PRIVATE)
}