package ru.mtuci.drivenext.domain.repository

import ru.mtuci.drivenext.domain.model.Car
import ru.mtuci.drivenext.domain.model.UserProfile

interface CatalogRepository {
    fun userId(): String?
    fun savedQuery(): String
    fun saveQuery(query: String)
    suspend fun cars(query: String): List<Car>
    suspend fun profile(): UserProfile
    suspend fun avatar(path: String): ByteArray
    suspend fun updateAvatar(uri: String): UserProfile
    suspend fun signOut()
}
