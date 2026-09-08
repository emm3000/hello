package com.emm.hello.core.auth

interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleSignInResult
}
