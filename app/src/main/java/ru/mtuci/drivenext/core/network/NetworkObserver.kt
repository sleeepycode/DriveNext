package ru.mtuci.drivenext.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class NetworkObserver(context: Context) {

    private val appContext = context.applicationContext

    private val connectivityManager =
        appContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    private val networkChecker =
        NetworkChecker(appContext)

    @OptIn(FlowPreview::class)
    fun observe(): Flow<Boolean> = callbackFlow {

        val callback = object :
            ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                trySend(Unit)
            }

            override fun onLost(network: Network) {
                trySend(Unit)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(Unit)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(
            callback
        )

        trySend(Unit)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(
                callback
            )
        }

    }
        .debounce(300)
        .map { networkChecker.isInternetAvailable() }
        .distinctUntilChanged()
}