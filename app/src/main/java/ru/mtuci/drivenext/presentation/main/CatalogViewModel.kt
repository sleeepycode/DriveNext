package ru.mtuci.drivenext.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.data.remote.SupabaseProvider
import ru.mtuci.drivenext.data.repository.CatalogRepositoryImpl
import ru.mtuci.drivenext.domain.model.Car
import ru.mtuci.drivenext.domain.model.CatalogQuery
import ru.mtuci.drivenext.domain.model.UserProfile
import ru.mtuci.drivenext.domain.usecase.CatalogUseCases
import ru.mtuci.drivenext.domain.usecase.RentalUseCases
import ru.mtuci.drivenext.data.repository.RentalRepositoryImpl
import ru.mtuci.drivenext.domain.model.CarDetails
import ru.mtuci.drivenext.domain.model.RentalDraft
import ru.mtuci.drivenext.domain.model.Booking
import java.time.Instant
import java.time.temporal.ChronoUnit

class CatalogViewModel(private val useCases: CatalogUseCases, private val rental: RentalUseCases? = null) : ViewModel() {
    enum class Screen { HOME, SEARCH, SAVED, SETTINGS, PROFILE, DETAILS, RENT, BOOKINGS, BOOKING_DETAILS, SUCCESS, ADD_CAR }
    data class Ui(
        val screen: Screen = Screen.HOME,
        val query: String = "",
        val cars: List<Car> = emptyList(),
        val profile: UserProfile? = null,
        val avatar: ByteArray? = null,
        val selected: Car? = null,
        val busy: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val signedOut: Boolean = false,
        val details: CarDetails? = null,
        val favorite: Boolean = false,
        val rentalDraft: RentalDraft? = null,
        val bookings: List<Booking> = emptyList(),
        val booking: Booking? = null
    )
    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()
    private var owner: String? = null
    private var job: Job? = null
    private var returnScreen = Screen.HOME
    private var mutation = false
    private var rentFromDetails = false
    private var bookingsFromSuccess = false

    fun open() {
        val id = useCases.userId() ?: return
        if (owner != id) {
            job?.cancel()
            owner = id
            mutation = false
            val query = useCases.savedQuery()
            _ui.value = Ui(screen = if (query.isBlank()) Screen.HOME else Screen.SEARCH, query = query)
        }
        if (!mutation) retry()
    }
    fun home() { if (!mutation) { useCases.saveQuery(""); show(Screen.HOME); loadCars("") } }
    fun saved() { if (!mutation) { show(Screen.SAVED); loadSaved() } }
    fun settings() { if (!mutation) { show(Screen.SETTINGS); loadProfile() } }
    fun profile() { if (!mutation) { show(Screen.PROFILE); loadProfile() } }
    fun bookings() { if (!mutation) { bookingsFromSuccess = _ui.value.screen == Screen.SUCCESS; show(Screen.BOOKINGS); loadBookings() } }
    fun addCar() { if (!mutation) show(Screen.ADD_CAR) }
    fun car(car: Car, rent: Boolean) {
        if (mutation) return
        returnScreen = _ui.value.screen
        rentFromDetails = false
        show(if (rent) Screen.RENT else Screen.DETAILS)
        _ui.value = _ui.value.copy(selected = car, details = null, rentalDraft = null, favorite = false)
        loadDetails(rent)
    }
    fun rentSelected() {
        if (mutation || _ui.value.details == null) return
        rentFromDetails = true
        show(Screen.RENT)
        _ui.value = _ui.value.copy(rentalDraft = null)
        loadDetails(true)
    }
    private fun loadDetails(rent: Boolean) {
        val selected = _ui.value.selected ?: return
        val r = rental ?: return
        loadRental {
            val details = r.details(selected.id)
            val favorite = selected.id in r.favoriteIds()
            val draft = if (rent && _ui.value.rentalDraft == null) {
                val driver = r.driver()
                val start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES)
                RentalDraft(start, start.plus(1, ChronoUnit.DAYS), driver.first, driver.second)
            } else _ui.value.rentalDraft
            _ui.value = _ui.value.copy(details = details, favorite = favorite, rentalDraft = draft)
        }
    }
    private fun loadSaved() {
        val r = rental ?: return
        _ui.value = _ui.value.copy(cars = emptyList())
        loadRental { val ids = r.favoriteIds().toSet(); _ui.value = _ui.value.copy(cars = useCases.cars("").filter { it.id in ids }) }
    }
    private fun loadBookings() {
        val r = rental ?: return
        loadRental { _ui.value = _ui.value.copy(bookings = r.bookings()) }
    }
    fun booking(item: Booking) {
        if (mutation) return
        show(Screen.BOOKING_DETAILS); _ui.value = _ui.value.copy(booking = item)
    }
    fun toggleFavorite() {
        val r = rental ?: return
        val d = _ui.value.details ?: return
        if (_ui.value.busy || mutation) return
        val next = !_ui.value.favorite
        loadRental(true, "Не удалось изменить избранное. Попробуйте снова.") { r.favorite(d.car.id, next); _ui.value = _ui.value.copy(favorite = next) }
    }
    fun changeDates(start: Instant? = null, end: Instant? = null) {
        if (mutation || _ui.value.screen != Screen.RENT) return
        val d = _ui.value.rentalDraft ?: return
        _ui.value = _ui.value.copy(rentalDraft = d.copy(start = start ?: d.start, end = end ?: d.end, requestId = java.util.UUID.randomUUID().toString()))
    }
    fun changeDriver(name: String? = null, license: String? = null) {
        if (mutation || _ui.value.screen != Screen.RENT) return
        val d = _ui.value.rentalDraft ?: return
        val updated = d.copy(driver = name ?: d.driver, license = license ?: d.license)
        if (updated != d) _ui.value = _ui.value.copy(rentalDraft = updated.copy(requestId = java.util.UUID.randomUUID().toString()))
    }
    fun submitRental() {
        val r = rental ?: return
        val d = _ui.value.details ?: return
        val draft = _ui.value.rentalDraft ?: return
        if (_ui.value.busy || mutation) return
        loadRental(true, "Не удалось создать бронирование. Попробуйте снова.") {
            r.book(d, draft)
            _ui.value = _ui.value.copy(screen = Screen.SUCCESS, selected = null, details = null, rentalDraft = null, booking = null)
            rentFromDetails = false; returnScreen = Screen.HOME
        }
    }
    fun cancelBooking() {
        val r = rental ?: return
        val b = _ui.value.booking ?: return
        if (_ui.value.busy || mutation || !b.active) return
        loadRental(true, "Не удалось отменить бронирование. Попробуйте снова.") {
            r.cancel(b.id)
            _ui.value = _ui.value.copy(screen = Screen.BOOKINGS, booking = null, bookings = emptyList())
            _ui.value = _ui.value.copy(bookings = r.bookings())
        }
    }
    private fun loadRental(write: Boolean = false, failure: String = "Не удалось загрузить данные. Попробуйте снова.", block: suspend () -> Unit) {
        if (mutation) return
        job?.cancel(); mutation = write
        _ui.value = _ui.value.copy(busy = true, error = null, message = null)
        job = viewModelScope.launch {
            try { block(); _ui.value = _ui.value.copy(busy = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = e.message?.takeIf { it.startsWith("Автомобиль") || it.startsWith("Проверьте") || it.startsWith("Ошибка сервера") } ?: failure
                _ui.value = if (write) _ui.value.copy(busy = false, message = message) else _ui.value.copy(busy = false, error = message)
            } finally { if (write) mutation = false }
        }
    }
    fun search(raw: String, immediate: Boolean = false) {
        if (mutation || _ui.value.screen !in listOf(Screen.HOME, Screen.SEARCH)) return
        val query = CatalogQuery.normalize(raw)
        if (query == _ui.value.query && !immediate) return
        useCases.saveQuery(query)
        // While editing, retain the search widget instead of recreating it.
        _ui.value = _ui.value.copy(screen = if (query.isBlank()) Screen.HOME else Screen.SEARCH,
            query = query, cars = emptyList(), error = null, busy = true)
        loadCars(query, if (immediate) 0 else 350)
    }
    fun back(): Boolean {
        if (mutation) return true
        when (_ui.value.screen) {
            Screen.HOME -> return false
            Screen.SEARCH, Screen.SETTINGS, Screen.SAVED -> home()
            Screen.PROFILE, Screen.ADD_CAR -> settings()
            Screen.BOOKINGS -> if (bookingsFromSuccess) home() else settings()
            Screen.BOOKING_DETAILS -> { show(Screen.BOOKINGS); loadBookings() }
            Screen.SUCCESS -> home()
            Screen.DETAILS, Screen.RENT -> {
                if (_ui.value.screen == Screen.RENT && rentFromDetails) { show(Screen.DETAILS); loadDetails(false) }
                else { show(returnScreen); if (returnScreen == Screen.SAVED) loadSaved() else loadCars(if (returnScreen == Screen.SEARCH) _ui.value.query else "") }
            }
        }
        return true
    }
    fun retry() {
        if (mutation) return
        when (_ui.value.screen) {
            Screen.HOME -> loadCars("")
            Screen.SEARCH -> loadCars(_ui.value.query)
            Screen.SETTINGS, Screen.PROFILE -> loadProfile()
            Screen.SAVED -> loadSaved()
            Screen.DETAILS -> loadDetails(false)
            Screen.RENT -> loadDetails(true)
            Screen.BOOKINGS -> loadBookings()
            else -> Unit
        }
    }
    private fun show(screen: Screen) {
        job?.cancel()
        _ui.value = _ui.value.copy(screen = screen, busy = false, error = null, message = null)
    }
    private fun loadCars(query: String, wait: Long = 0) {
        job?.cancel()
        _ui.value = _ui.value.copy(query = query, busy = true, cars = emptyList(), error = null)
        job = viewModelScope.launch {
            try {
                delay(wait)
                val cars = useCases.cars(query)
                _ui.value = _ui.value.copy(cars = cars, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _ui.value = _ui.value.copy(busy = false, error = if (query.isBlank())
                    "Не удалось загрузить данные. Попробуйте снова." else "Не удалось выполнить поиск. Попробуйте снова.")
            }
        }
    }
    private fun loadProfile() {
        job?.cancel()
        _ui.value = _ui.value.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                val profile = useCases.profile()
                _ui.value = _ui.value.copy(profile = profile)
                val avatar = profile.avatarPath?.let { useCases.avatar(it) }
                _ui.value = _ui.value.copy(profile = profile, avatar = avatar, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _ui.value = _ui.value.copy(busy = false, error = "Не удалось загрузить данные. Попробуйте снова.") }
        }
    }
    fun updateAvatar(uri: String) {
        if (mutation || _ui.value.busy || _ui.value.screen != Screen.PROFILE) return
        job?.cancel()
        mutation = true
        _ui.value = _ui.value.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                val profile = useCases.updateAvatar(uri)
                val avatar = profile.avatarPath?.let { useCases.avatar(it) }
                _ui.value = _ui.value.copy(profile = profile, avatar = avatar, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _ui.value = _ui.value.copy(busy = false, message = "Не удалось обновить аватар. Попробуйте снова.") }
            finally { mutation = false }
        }
    }
    fun signOut() {
        if (mutation) return
        job?.cancel()
        mutation = true
        _ui.value = _ui.value.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                useCases.signOut()
                owner = null
                _ui.value = Ui(signedOut = true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _ui.value = _ui.value.copy(busy = false, message = "Не удалось выйти из профиля. Попробуйте снова.") }
            finally { mutation = false }
        }
    }
    fun consumeSignOut() { _ui.value = Ui(); owner = null }
    fun dismissMessage() { _ui.value = _ui.value.copy(message = null) }
    class Factory(context: Context) : ViewModelProvider.Factory {
        private val app = context.applicationContext
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
            return CatalogViewModel(CatalogUseCases(CatalogRepositoryImpl(SupabaseProvider.client, app)), RentalUseCases(RentalRepositoryImpl(SupabaseProvider.client, app))) as T
        }
    }
}
