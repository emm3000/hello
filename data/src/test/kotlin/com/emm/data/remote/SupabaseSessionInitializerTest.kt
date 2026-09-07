package com.emm.data.remote

import com.emm.domain.generation.SessionExpiredException
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSessionInitializerTest {

    private val auth: Auth = mockk()
    private val status: MutableStateFlow<SessionStatus> = MutableStateFlow(SessionStatus.Initializing)
    private val initializer: SupabaseSessionInitializer = SupabaseSessionInitializer(auth)

    init {
        every { auth.sessionStatus } returns status
        coEvery { auth.awaitInitialization() } returns Unit
        every { auth.currentSessionOrNull() } answers { authenticatedSession() }
    }

    @Test
    fun `a refresh failure never signs in anonymously and surfaces an expired session`() = runTest {
        status.value = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("offline")))
        coEvery { auth.refreshCurrentSession() } throws IOException("offline")

        val failure: Throwable? = runCatching { initializer.ensureSession() }.exceptionOrNull()

        assertTrue("expected a SessionExpiredException but was $failure", failure is SessionExpiredException)
        coVerify(exactly = 0) { auth.signInAnonymously(any(), any()) }
    }

    @Test
    fun `an expired access token is refreshed before the session is used`() = runTest {
        status.value = authenticated("stale-token", expiresIn = (-1).hours)
        coEvery { auth.refreshCurrentSession() } coAnswers {
            status.value = authenticated("fresh-token", expiresIn = 1.hours)
        }

        initializer.ensureSession()

        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    @Test
    fun `an access token inside the refresh leeway is refreshed before the session is used`() = runTest {
        status.value = authenticated("stale-token", expiresIn = 30.seconds)
        coEvery { auth.refreshCurrentSession() } coAnswers {
            status.value = authenticated("fresh-token", expiresIn = 1.hours)
        }

        initializer.ensureSession()

        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    @Test
    fun `a session well inside its lifetime is kept without a refresh`() = runTest {
        status.value = authenticated("valid-token", expiresIn = 1.hours)

        initializer.ensureSession()

        coVerify(exactly = 0) { auth.refreshCurrentSession() }
    }

    @Test
    fun `a missing session signs in anonymously exactly once`() = runTest {
        status.value = SessionStatus.NotAuthenticated()
        coEvery { auth.signInAnonymously(any(), any()) } coAnswers {
            status.value = authenticated("anonymous-token", expiresIn = 1.hours)
        }

        initializer.ensureSession()

        coVerify(exactly = 1) { auth.signInAnonymously(any(), any()) }
    }

    private fun authenticatedSession(): UserSession? {
        return (status.value as? SessionStatus.Authenticated)?.session
    }

    private fun authenticated(accessToken: String, expiresIn: Duration): SessionStatus.Authenticated {
        val expiresAt: Instant = Clock.System.now() + expiresIn
        return SessionStatus.Authenticated(
            UserSession(
                accessToken = accessToken,
                refreshToken = "refresh-token",
                expiresIn = expiresIn.inWholeSeconds,
                tokenType = "bearer",
                expiresAt = expiresAt,
            ),
        )
    }
}
