package ru.mtuci.drivenext.domain.repository

interface NetworkRepository {

    fun isInternetAvailable(): Boolean
}