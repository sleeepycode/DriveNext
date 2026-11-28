package ru.mtuci.drivenext.data.repository
import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import ru.mtuci.drivenext.data.local.FavoriteEntity
import ru.mtuci.drivenext.data.local.FavoritesDatabase
import ru.mtuci.drivenext.data.remote.RentalRemote
import ru.mtuci.drivenext.domain.model.RentalDraft
import ru.mtuci.drivenext.domain.repository.RentalRepository
class RentalRepositoryImpl(private val client: SupabaseClient, context: Context) : RentalRepository {
    private val dao = FavoritesDatabase.get(context).favorites()
    private val remote = RentalRemote()
    private fun uid() = requireNotNull(client.auth.currentUserOrNull()).id
    private fun token() = requireNotNull(client.auth.currentSessionOrNull()).accessToken
    override suspend fun details(id: Long) = remote.details(id, token())
    override suspend fun favoriteIds() = dao.ids(uid())
    override suspend fun favorite(id: Long, enabled: Boolean) { val owner = uid(); if (enabled) dao.insert(FavoriteEntity(owner,id)) else dao.remove(owner,id) }
    override suspend fun driver() = remote.driver(uid(), token())
    override suspend fun bookings() = remote.bookings(token())
    override suspend fun book(id: Long, draft: RentalDraft) = remote.book(id,draft,token())
    override suspend fun cancel(id: Long) = remote.cancel(id, token())
}
