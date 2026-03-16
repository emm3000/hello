package com.emm.hello.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log

object ConnectivitySyncTrigger {

    @Volatile
    private var isRegistered: Boolean = false

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun register(context: Context) {
        if (isRegistered) return
        synchronized(this) {
            if (isRegistered) return

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
                Log.w("ConnectivitySync", "Network callback registration failed: ${error.message}")
                return
            }

            networkCallback = callback
            isRegistered = true
        }
    }
}
