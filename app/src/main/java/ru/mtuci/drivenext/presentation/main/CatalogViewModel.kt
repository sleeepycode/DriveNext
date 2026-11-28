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

class CatalogViewModel(private val useCases: CatalogUseCases) : ViewModel() {
    enum class Screen { HOME, SEARCH, SAVED, SETTINGS, PROFILE, DETAILS, RENT, BOOKINGS, ADD_CAR }
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
        val signedOut: Boolean = false
    )
    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()
    private var owner: String? = null
    private var job: Job? = null
    private var returnScreen = Screen.HOME
    private var mutation = false

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
    fun saved() { if (!mutation) show(Screen.SAVED) }
    fun settings() { if (!mutation) { show(Screen.SETTINGS); loadProfile() } }
    fun profile() { if (!mutation) { show(Screen.PROFILE); loadProfile() } }
    fun bookings() { if (!mutation) show(Screen.BOOKINGS) }
    fun addCar() { if (!mutation) show(Screen.ADD_CAR) }
    fun car(car: Car, rent: Boolean) {
        if (mutation) return
        returnScreen = _ui.value.screen
        show(if (rent) Screen.RENT else Screen.DETAILS)
        _ui.value = _ui.value.copy(selected = car)
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
            Screen.PROFILE, Screen.BOOKINGS, Screen.ADD_CAR -> settings()
            Screen.DETAILS, Screen.RENT -> {
                show(returnScreen)
                loadCars(if (returnScreen == Screen.SEARCH) _ui.value.query else "")
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
            return CatalogViewModel(CatalogUseCases(CatalogRepositoryImpl(SupabaseProvider.client, app))) as T
        }
    }
}
