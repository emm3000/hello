package com.emm.data.remote

import com.emm.domain.generation.SessionExpiredException
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupabaseSessionInitializer(
    private val auth: Auth,
) : SessionInitializer {

    private val signInLock = Mutex()

    override suspend fun ensureSession() {
        signInLock.withLock {
            auth.awaitInitialization()
            when (auth.sessionStatus.value) {
                is SessionStatus.Authenticated -> refreshExpiringSession()
                is SessionStatus.NotAuthenticated -> auth.signInAnonymously()
                is SessionStatus.RefreshFailure -> recoverFailedSession()
                SessionStatus.Initializing -> error("Supabase auth is still initializing")
            }
            checkNotNull(auth.currentSessionOrNull()) { "No Supabase session" }
        }
    }

    private suspend fun refreshExpiringSession() {
        val session: UserSession = auth.currentSessionOrNull() ?: return
        if (session.expiresAt - REFRESH_LEEWAY <= Clock.System.now()) {
            auth.refreshCurrentSession()
        }
    }

    private suspend fun recoverFailedSession() {
        try {
            auth.refreshCurrentSession()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw SessionExpiredException(failure)
        }
    }

    private companion object {
        val REFRESH_LEEWAY: Duration = 60.seconds
    }
}
