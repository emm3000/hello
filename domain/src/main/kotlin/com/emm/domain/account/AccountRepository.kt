package com.emm.domain.account

interface AccountRepository {

    suspend fun currentAccount(): Account?

    suspend fun linkGoogleAccount(idToken: String, rawNonce: String): AccountLinkResult
}
