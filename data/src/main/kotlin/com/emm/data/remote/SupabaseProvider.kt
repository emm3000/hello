package com.emm.data.remote

import com.emm.data.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.koin.dsl.module

fun provideSupabaseClient(): SupabaseClient {
    val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
    val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    require(supabaseUrl.isNotBlank()) {
        "Missing SUPABASE_URL in local.properties"
    }
    require(supabaseAnonKey.isNotBlank()) {
        "Missing SUPABASE_ANON_KEY in local.properties"
    }

    return createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseAnonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
        install(Realtime)
    }
}

val supabaseModule = module {
    single { provideSupabaseClient() }
}
