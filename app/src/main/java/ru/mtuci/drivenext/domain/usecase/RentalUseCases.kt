package ru.mtuci.drivenext.domain.usecase
import ru.mtuci.drivenext.domain.model.*
import ru.mtuci.drivenext.domain.repository.RentalRepository
class RentalUseCases(private val repository: RentalRepository) {
    suspend fun details(id: Long) = repository.details(id)
    suspend fun favoriteIds() = repository.favoriteIds()
    suspend fun favorite(id: Long, enabled: Boolean) = repository.favorite(id, enabled)
    suspend fun driver() = repository.driver()
    suspend fun bookings() = repository.bookings()
    suspend fun book(details: CarDetails, draft: RentalDraft) {
        require(RentalRules.valid(details, draft)) { "Проверьте даты аренды, ФИО и номер удостоверения (10 символов)." }
        repository.book(details.car.id, draft)
    }
    suspend fun cancel(id: Long) = repository.cancel(id)
}
