package com.emm.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupabaseSessionInitializer(
    private val client: SupabaseClient,
) : SessionInitializer {

    private val signInLock = Mutex()

    override suspend fun ensureSession(): String = signInLock.withLock {
        client.auth.sessionStatus.first { status -> status !is SessionStatus.Initializing }
        if (client.auth.currentSessionOrNull() == null) {
            client.auth.signInAnonymously()
        }
        client.auth.currentAccessTokenOrNull() ?: error("No Supabase session")
    }
}
