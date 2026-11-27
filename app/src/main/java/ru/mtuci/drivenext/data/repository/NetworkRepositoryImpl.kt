package ru.mtuci.drivenext.data.repository

import ru.mtuci.drivenext.core.network.NetworkChecker
import ru.mtuci.drivenext.domain.repository.NetworkRepository

class NetworkRepositoryImpl(
    private val networkChecker: NetworkChecker
) : NetworkRepository {

    override fun isInternetAvailable(): Boolean {
        return networkChecker.isInternetAvailable()
    }
}