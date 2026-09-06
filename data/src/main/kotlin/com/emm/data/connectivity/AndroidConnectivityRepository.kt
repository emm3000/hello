package com.emm.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.emm.domain.connectivity.ConnectivityRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidConnectivityRepository(private val applicationContext: Context) : ConnectivityRepository {

    override fun observeOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager: ConnectivityManager? =
            applicationContext.getSystemService(ConnectivityManager::class.java)
        trySend(connectivityManager?.isOnline() ?: false)

        val callback: ConnectivityManager.NetworkCallback = DefaultNetworkCallback { online -> trySend(online) }
        connectivityManager?.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

private class DefaultNetworkCallback(
    private val onOnlineChanged: (Boolean) -> Unit,
) : ConnectivityManager.NetworkCallback() {

    private var currentNetwork: Network? = null

    override fun onAvailable(network: Network) {
        currentNetwork = network
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        if (network == currentNetwork) onOnlineChanged(networkCapabilities.hasValidatedInternet())
    }

    override fun onLost(network: Network) {
        if (network != currentNetwork) return
        currentNetwork = null
        onOnlineChanged(false)
    }
}

private fun ConnectivityManager.isOnline(): Boolean =
    getNetworkCapabilities(activeNetwork)?.hasValidatedInternet() ?: false

private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
