package com.emm.hello.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log

object ConnectivitySyncTrigger {

    @Volatile private var isRegistered: Boolean = false

    // Separate flag so a failed registration also prevents future retry attempts.
    // registerDefaultNetworkCallback() failure is unrecoverable at runtime — retrying
    // in rapid succession would only generate log noise without fixing anything.
    @Volatile private var registrationFailed: Boolean = false

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun register(context: Context) {
        if (isRegistered || registrationFailed) return
        synchronized(this) {
            if (isRegistered || registrationFailed) return

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Sync.requestImmediate(context)
                }
            }

            runCatching {
                connectivityManager.registerDefaultNetworkCallback(callback)
            }.onFailure { error ->
                Log.w("ConnectivitySync", "Network callback registration failed — will not retry: ${error.message}")
                registrationFailed = true
                return
            }

            networkCallback = callback
            isRegistered = true
        }
    }
}
