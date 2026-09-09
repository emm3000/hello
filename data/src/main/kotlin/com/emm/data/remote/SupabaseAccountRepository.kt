package com.emm.data.remote

import com.emm.domain.account.Account
import com.emm.domain.account.AccountLinkResult
import com.emm.domain.account.AccountRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo

class SupabaseAccountRepository(
    private val auth: Auth,
    private val sessionInitializer: SessionInitializer,
) : AccountRepository {

    override suspend fun currentAccount(): Account? {
        auth.awaitInitialization()
        return auth.currentUserOrNull()?.toAccount()
    }

    override suspend fun linkGoogleAccount(idToken: String, rawNonce: String): AccountLinkResult {
        sessionInitializer.ensureSession()
        return try {
            auth.linkIdentityWithIdToken(provider = Google, idToken = idToken) { nonce = rawNonce }
            AccountLinkResult.Linked(refreshedAccount())
        } catch (linkFailure: AuthRestException) {
            if (linkFailure.errorCode != AuthErrorCode.IdentityAlreadyExists) throw linkFailure
            signInWithGoogleIdToken(idToken = idToken, rawNonce = rawNonce)
            AccountLinkResult.SwitchedToExisting(refreshedAccount())
        }
    }

    private suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String) {
        auth.signInWith(IDToken, redirectUrl = null) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    private suspend fun refreshedAccount(): Account {
        val user: UserInfo = auth.retrieveUserForCurrentSession(updateSession = true)
        return user.toAccount()
    }

    private fun UserInfo.toAccount(): Account = Account(
        id = id,
        isAnonymous = isAnonymous == true,
        email = email,
    )
}
