package ru.mtuci.drivenext.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import ru.mtuci.drivenext.data.remote.CatalogRemote
import ru.mtuci.drivenext.data.remote.ProfileRemote
import ru.mtuci.drivenext.domain.repository.CatalogRepository

class CatalogRepositoryImpl(private val client: SupabaseClient, context: Context) : CatalogRepository {
    private val remote = CatalogRemote()
    private val photos = ProfileRemote(context.applicationContext)
    private val preferences = context.getSharedPreferences("drivenext_catalog", Context.MODE_PRIVATE)
    private val auth get() = client.auth
    override fun userId() = auth.currentUserOrNull()?.id
    private fun id() = requireNotNull(userId()) { "Войдите в аккаунт." }
    private fun token() = requireNotNull(auth.currentSessionOrNull()) { "Войдите в аккаунт." }.accessToken
    override fun savedQuery() = preferences.getString("query_${id()}", "").orEmpty()
    override fun saveQuery(query: String) { preferences.edit().putString("query_${id()}", query).apply() }
    override suspend fun cars(query: String) = remote.cars(query, token())
    override suspend fun profile() = remote.profile(id(), token())
    override suspend fun avatar(path: String) = remote.avatar(id(), path, token())
    override suspend fun updateAvatar(uri: String): ru.mtuci.drivenext.domain.model.UserProfile {
        val user = id()
        val path = photos.upload(user, "avatar", uri, token())
        remote.updateAvatar(user, path, token())
        return profile()
    }
    override suspend fun signOut() {
        auth.signOut()
    }
}
