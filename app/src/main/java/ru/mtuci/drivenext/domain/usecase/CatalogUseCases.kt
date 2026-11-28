package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.model.CatalogQuery
import ru.mtuci.drivenext.domain.repository.CatalogRepository

class CatalogUseCases(private val repository: CatalogRepository) {
    fun userId() = repository.userId()
    fun savedQuery() = repository.savedQuery()
    fun saveQuery(query: String) = repository.saveQuery(CatalogQuery.normalize(query))
    suspend fun cars(query: String) = repository.cars(CatalogQuery.normalize(query))
    suspend fun profile() = repository.profile()
    suspend fun avatar(path: String) = repository.avatar(path)
    suspend fun updateAvatar(uri: String) = repository.updateAvatar(uri)
    suspend fun signOut() = repository.signOut()
}
