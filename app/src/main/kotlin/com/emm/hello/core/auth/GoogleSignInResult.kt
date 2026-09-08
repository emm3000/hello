package com.emm.hello.core.auth

sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val rawNonce: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredentials : GoogleSignInResult
    data class Failure(val cause: Throwable) : GoogleSignInResult
}
