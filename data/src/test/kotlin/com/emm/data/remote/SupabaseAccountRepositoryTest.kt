package com.emm.data.remote

import com.emm.domain.account.Account
import com.emm.domain.account.AccountLinkResult
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SupabaseAccountRepositoryTest {

    private val auth: Auth = mockk {
        coEvery { awaitInitialization() } returns Unit
    }
    private val sessionInitializer = RecordingSessionInitializer()
    private val repository: SupabaseAccountRepository = SupabaseAccountRepository(auth, sessionInitializer)

    @Test
    fun `reading the current account never creates a remote session`() = runTest {
        every { auth.currentUserOrNull() } returns anonymousUser()

        val account: Account? = repository.currentAccount()

        assertEquals(Account(id = "user-1", isAnonymous = true, email = null), account)
        assertEquals(0, sessionInitializer.ensureSessionCalls)
    }

    @Test
    fun `reading the current account waits for the stored session to load first`() = runTest {
        val restored: MutableList<String> = mutableListOf()
        coEvery { auth.awaitInitialization() } answers { restored += "awaited" }
        every { auth.currentUserOrNull() } answers {
            restored += "read"
            linkedUser()
        }

        repository.currentAccount()

        assertEquals(listOf("awaited", "read"), restored)
    }

    @Test
    fun `reading the current account without a session returns null and never creates one`() = runTest {
        every { auth.currentUserOrNull() } returns null

        val account: Account? = repository.currentAccount()

        assertNull(account)
        assertEquals(0, sessionInitializer.ensureSessionCalls)
    }

    @Test
    fun `linking Google ensures a session and passes the raw nonce through the config block`() = runTest {
        val config = slot<IDToken.Config.() -> Unit>()
        coEvery { auth.linkIdentityWithIdToken(Google, "id-token", capture(config)) } returns Unit
        coEvery { auth.retrieveUserForCurrentSession(updateSession = true) } returns linkedUser()

        val result: AccountLinkResult = repository.linkGoogleAccount(idToken = "id-token", rawNonce = "raw-nonce")

        assertEquals(1, sessionInitializer.ensureSessionCalls)
        val captured = IDToken.Config()
        captured.apply(config.captured)
        assertEquals("raw-nonce", captured.nonce)
        val linked = Account(id = "user-1", isAnonymous = false, email = "someone@example.com")
        assertEquals(AccountLinkResult.Linked(linked), result)
        coVerify(exactly = 1) { auth.retrieveUserForCurrentSession(updateSession = true) }
    }

    @Test
    fun `linking a Google identity that belongs to another user signs in to that user instead`() = runTest {
        val config = slot<IDToken.Config.() -> Unit>()
        coEvery {
            auth.linkIdentityWithIdToken(Google, "id-token", any())
        } throws authRestException(AuthErrorCode.IdentityAlreadyExists)
        coEvery { auth.signInWith(IDToken, null, capture(config)) } returns Unit
        coEvery { auth.retrieveUserForCurrentSession(updateSession = true) } returns otherUser()

        val result: AccountLinkResult = repository.linkGoogleAccount(idToken = "id-token", rawNonce = "raw-nonce")

        coVerify(exactly = 1) { auth.signInWith(IDToken, null, any()) }
        val captured = IDToken.Config()
        captured.apply(config.captured)
        assertEquals("id-token", captured.idToken)
        assertEquals("raw-nonce", captured.nonce)
        assertEquals(Google, captured.provider)
        val owner = Account(id = "user-2", isAnonymous = false, email = "owner@example.com")
        assertEquals(AccountLinkResult.SwitchedToExisting(owner), result)
        assertEquals(1, sessionInitializer.ensureSessionCalls)
    }

    @Test
    fun `linking with any other auth error propagates it`() = runTest {
        val failure: AuthRestException = authRestException(AuthErrorCode.BadJwt)
        coEvery { auth.linkIdentityWithIdToken(Google, "id-token", any()) } throws failure
        coEvery { auth.retrieveUserForCurrentSession(updateSession = true) } returns linkedUser()

        val thrown: Throwable? = runCatching {
            repository.linkGoogleAccount(idToken = "id-token", rawNonce = "raw-nonce")
        }.exceptionOrNull()

        assertSame(failure, thrown)
        coVerify(exactly = 0) { auth.signInWith(IDToken, any(), any()) }
    }

    private suspend fun authRestException(code: AuthErrorCode): AuthRestException = AuthRestException(
        errorCode = code.value,
        errorDescription = "the auth api rejected the request",
        response = conflictResponse(),
    )

    private suspend fun conflictResponse(): HttpResponse {
        val client = HttpClient(MockEngine { respond(content = "", status = HttpStatusCode.Conflict) })
        return client.get("https://hello.test/auth/v1/user")
    }

    private fun anonymousUser(): UserInfo = UserInfo(
        aud = "authenticated",
        id = "user-1",
        isAnonymous = true,
    )

    private fun linkedUser(): UserInfo = UserInfo(
        aud = "authenticated",
        id = "user-1",
        email = "someone@example.com",
        isAnonymous = false,
    )

    private fun otherUser(): UserInfo = UserInfo(
        aud = "authenticated",
        id = "user-2",
        email = "owner@example.com",
        isAnonymous = false,
    )
}

private class RecordingSessionInitializer : SessionInitializer {

    var ensureSessionCalls: Int = 0
        private set

    override suspend fun ensureSession() {
        ensureSessionCalls += 1
    }
}
