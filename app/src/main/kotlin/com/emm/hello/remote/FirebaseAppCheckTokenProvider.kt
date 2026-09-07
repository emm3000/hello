package com.emm.hello.remote

import com.emm.data.remote.AppCheckTokenProvider
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAppCheckTokenProvider : AppCheckTokenProvider {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun token(): String {
        return try {
            val token: AppCheckToken = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            token.token
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            throw IOException("App Check token unavailable", error)
        }
    }
}
