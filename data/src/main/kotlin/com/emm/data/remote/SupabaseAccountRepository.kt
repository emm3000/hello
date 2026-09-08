package com.emm.data.remote

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserInfo

class SupabaseAccountRepository(
    private val auth: Auth,
    private val sessionInitializer: SessionInitializer,
) : AccountRepository {

    override suspend fun currentAccount(): Account? {
        auth.awaitInitialization()
        return auth.currentUserOrNull()?.toAccount()
    }

    override suspend fun linkGoogleAccount(idToken: String, rawNonce: String): Account {
        sessionInitializer.ensureSession()
        auth.linkIdentityWithIdToken(provider = Google, idToken = idToken) { nonce = rawNonce }
        val linked: UserInfo = auth.retrieveUserForCurrentSession(updateSession = true)
        return linked.toAccount()
    }

    private fun UserInfo.toAccount(): Account = Account(
        id = id,
        isAnonymous = isAnonymous == true,
        email = email,
    )
}
