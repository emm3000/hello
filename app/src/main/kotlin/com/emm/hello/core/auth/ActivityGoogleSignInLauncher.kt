package com.emm.hello.core.auth

import android.app.Activity
import com.emm.hello.core.activity.CurrentActivityHolder

class ActivityGoogleSignInLauncher(
    private val activityHolder: CurrentActivityHolder,
    private val client: GoogleCredentialClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(serverClientId: String): GoogleSignInResult {
        val activity: Activity? = activityHolder.currentActivity
        return if (activity == null) {
            GoogleSignInResult.Failure(IllegalStateException("No foreground activity to launch credential sheet"))
        } else {
            client.signIn(activity, serverClientId)
        }
    }
}
