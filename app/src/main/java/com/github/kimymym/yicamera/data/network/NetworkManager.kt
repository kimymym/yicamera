package com.github.kimymym.yicamera.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "YiNetworkManager"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private var activeCameraNetwork: Network? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                Log.d(TAG, "Wi-Fi network detected, binding process to network: $network")
                activeCameraNetwork = network
                bindProcessToCameraNetwork(network)
                _isWifiConnected.value = true
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (activeCameraNetwork == network) {
                Log.d(TAG, "Wi-Fi network lost, unbinding process")
                bindProcessToCameraNetwork(null)
                activeCameraNetwork = null
                _isWifiConnected.value = false
            }
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    /**
     * 앱 프로세스의 모든 소켓 통신(TCP 7878, RTSP 554 등)을 Wi-Fi 네트워크로 강제 라우팅
     */
    fun bindProcessToCameraNetwork(network: Network?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(network)
        } else {
            @Suppress("DEPRECATION")
            ConnectivityManager.setProcessDefaultNetwork(network)
        }
    }

    /**
     * 카메라 Wi-Fi 네트워크를 강제 사용하여 URL 연결 생성
     */
    fun openConnection(url: java.net.URL): java.net.URLConnection {
        val net = activeCameraNetwork
        return if (net != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            net.openConnection(url)
        } else {
            url.openConnection()
        }
    }
}
