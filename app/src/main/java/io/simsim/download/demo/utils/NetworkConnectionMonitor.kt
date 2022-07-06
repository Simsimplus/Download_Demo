@file:Suppress("DEPRECATION")

package io.simsim.download.demo.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import splitties.systemservices.connectivityManager

/**
 * Listens for WIFI available/not available events. This
 * allows calling code to set simpler listeners.
 */
object NetworkConnectionMonitor : CoroutineScope by MainScope() {

    private var isWifiCallbackRegistered = false
        @Synchronized set

    private var isGeneralCallbackRegistered = false
        @Synchronized set

    val wifiConnectionFlow = callbackFlow {
        trySend(isWifiConnected())
        var frameworkListener: ConnectivityManager.NetworkCallback? = null
        if (!isWifiCallbackRegistered) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            frameworkListener = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    trySend(false)
                }

                override fun onAvailable(network: Network) {
                    trySend(true)
                }
            }
            connectivityManager.registerNetworkCallback(request, frameworkListener)
            isWifiCallbackRegistered = true
        }
        awaitClose {
            frameworkListener?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
        }
    }.distinctUntilChanged()

    /**
     * cold flow of network connection
     * @author Simsim
     * @since 2022/5/10
     */
    val connectionFlow = callbackFlow {
        trySend(isAvailable())
        var frameworkListener: ConnectivityManager.NetworkCallback? = null
        if (!isGeneralCallbackRegistered) {
            val request = NetworkRequest.Builder().build()
            frameworkListener = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    trySend(false)
                }

                override fun onAvailable(network: Network) {
                    trySend(true)
                }
            }
            connectivityManager.registerNetworkCallback(request, frameworkListener)
            isGeneralCallbackRegistered = true
        }
        awaitClose {
            frameworkListener?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
        }
    }.distinctUntilChanged().shareIn(this, SharingStarted.Lazily, replay = 1)
}
