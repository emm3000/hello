package com.emm.hello.di

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.data.remote.SupabaseFunctionsTransport
import com.emm.data.remote.SupabaseSessionInitializer
import com.emm.domain.telemetry.GenerationTelemetry
import com.emm.hello.BuildConfig
import com.emm.hello.analytics.FirebaseProductAnalytics
import com.emm.hello.analytics.LogcatProductAnalytics
import com.emm.hello.analytics.ProductAnalytics
import com.emm.hello.remote.FirebaseAppCheckTokenProvider
import com.emm.hello.telemetry.CrashlyticsGenerationTelemetry
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                enableLifecycleCallbacks = false
            }
            install(Functions)
        }
    }
    single<Auth> { get<SupabaseClient>().auth }
    single<FunctionsTransport> { SupabaseFunctionsTransport(get<SupabaseClient>().functions) }
    single<SessionInitializer> { SupabaseSessionInitializer(get()) }
    single<AppCheckTokenProvider> { FirebaseAppCheckTokenProvider() }
    single<GenerationTelemetry> { CrashlyticsGenerationTelemetry() }
    single<ProductAnalytics> {
        if (BuildConfig.DEBUG) {
            LogcatProductAnalytics()
        } else {
            FirebaseProductAnalytics(FirebaseAnalytics.getInstance(androidContext()))
        }
    }
}
