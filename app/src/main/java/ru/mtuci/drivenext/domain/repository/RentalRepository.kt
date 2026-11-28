package ru.mtuci.drivenext.domain.repository
import ru.mtuci.drivenext.domain.model.*
interface RentalRepository {
    suspend fun details(id: Long): CarDetails
    suspend fun favoriteIds(): List<Long>
    suspend fun favorite(id: Long, enabled: Boolean)
    suspend fun driver(): Pair<String, String>
    suspend fun bookings(): List<Booking>
    suspend fun book(id: Long, draft: RentalDraft)
    suspend fun cancel(id: Long)
}
