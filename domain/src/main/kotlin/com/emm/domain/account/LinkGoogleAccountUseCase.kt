package com.emm.domain.account

class LinkGoogleAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(idToken: String, rawNonce: String): AccountLinkResult {
        return repository.linkGoogleAccount(idToken = idToken, rawNonce = rawNonce)
    }
}
