@file:Suppress("deprecated", "unused")

package io.simsim.download.demo.utils

import android.net.NetworkCapabilities
import splitties.systemservices.connectivityManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

/**
 * 判断是否联网
 */
fun isAvailable(): Boolean = isWifiConnected() || isCellConnected()

/**
 * 判断是否WiFi联网
 */
fun isWifiConnected(): Boolean {
    val network = connectivityManager.activeNetwork
    if (network != null) {
        val nc = connectivityManager.getNetworkCapabilities(network)
        if (nc != null) {
            // 移动数据
            return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
    return false
}

/**
 * 判断是否流量联网
 */
fun isCellConnected(): Boolean {
    val network = connectivityManager.activeNetwork
    if (network != null) {
        val nc = connectivityManager.getNetworkCapabilities(network)
        if (nc != null) {
            // 移动数据
            return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }
    return false
}

/**
 * Get IP address from first non-localhost interface
 * @param useIPv4   true=return ipv4, false=return ipv6
 * @return address or empty string
 */
fun getIPAddress(useIPv4: Boolean = true): String {
    try {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        for (networkInterface in networkInterfaces) {
            val inetAddresses: List<InetAddress> = Collections.list(networkInterface.inetAddresses)
            for (inetAddress in inetAddresses) {
                if (!inetAddress.isLoopbackAddress) {
                    val hostAddress: String = inetAddress.hostAddress ?: "0.0.0.0"
                    val isIPv4 = hostAddress.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return hostAddress
                    } else {
                        if (!isIPv4) {
                            val delim = hostAddress.indexOf('%') // drop ip6 zone suffix
                            return if (delim < 0) hostAddress.uppercase(Locale.getDefault()) else hostAddress.substring(
                                0,
                                delim
                            ).uppercase(
                                Locale.getDefault()
                            )
                        }
                    }
                }
            }
        }
    } catch (ignored: Exception) {
    } // for now eat exceptions
    return ""
}
