package com.emm.hello.core.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.emm.hello.logging.logError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "GoogleCredentialClient"

class GoogleCredentialClient {

    @Suppress("TooGenericExceptionCaught")
    suspend fun signIn(activityContext: Context, serverClientId: String): GoogleSignInResult = try {
        val rawNonce: String = UUID.randomUUID().toString()
        val hashedNonce: String = sha256Hex(rawNonce)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response: GetCredentialResponse = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)

        val credential: Credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential: GoogleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(idToken = googleCredential.idToken, rawNonce = rawNonce)
        } else {
            GoogleSignInResult.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        logError(TAG, "signIn:noCredential ${e.message}", e)
        GoogleSignInResult.NoCredentials
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GoogleSignInResult.Failure(e)
    }

    private fun sha256Hex(input: String): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val bytes: ByteArray = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
