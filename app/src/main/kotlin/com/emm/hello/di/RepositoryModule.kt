package com.emm.hello.di

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.HttpFunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.data.remote.SupabaseSessionInitializer
import com.emm.domain.telemetry.GenerationTelemetry
import com.emm.hello.BuildConfig
import com.emm.hello.remote.FirebaseAppCheckTokenProvider
import com.emm.hello.telemetry.CrashlyticsGenerationTelemetry
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import org.koin.dsl.module

private const val REQUEST_TIMEOUT_MS: Long = 100_000L

val repositoryModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth)
        }
    }
    single {
        HttpClient(Android) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }
    }
    single<FunctionsTransport> {
        HttpFunctionsTransport(
            httpClient = get(),
            baseUrl = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
    }
    single<SessionInitializer> { SupabaseSessionInitializer(get()) }
    single<AppCheckTokenProvider> { FirebaseAppCheckTokenProvider() }
    single<GenerationTelemetry> { CrashlyticsGenerationTelemetry() }
}
