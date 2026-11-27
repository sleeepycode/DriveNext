package ru.mtuci.drivenext.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.data.remote.SupabaseProvider
import ru.mtuci.drivenext.data.repository.AccountRepositoryImpl
import ru.mtuci.drivenext.domain.model.AuthValidation
import ru.mtuci.drivenext.domain.model.RegistrationDraft
import ru.mtuci.drivenext.domain.usecase.AccountUseCases

class AccountViewModel(private val useCases: AccountUseCases) : ViewModel() {
    enum class Screen { WELCOME, LOGIN, REGISTER1, REGISTER2, REGISTER3, SUCCESS, RESET, DONE }
    data class Ui(val screen: Screen = Screen.WELCOME, val busy: Boolean = false, val message: String? = null)
    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()
    var draft = RegistrationDraft()
    var loginEmail = ""
    var loginPassword = ""
    var resetPassword = ""
    var resetConfirm = ""
    var photoTarget = ""
    var pendingPhoto: String? = null
    private var registrationOrigin = Screen.WELCOME
    fun navigate(screen: Screen) { if (!_ui.value.busy) _ui.value = Ui(screen) }
    fun startRegistration() {
        registrationOrigin = _ui.value.screen
        navigate(Screen.REGISTER1)
    }
    fun back() {
        if (_ui.value.busy) return
        navigate(when (_ui.value.screen) {
            Screen.LOGIN -> Screen.WELCOME
            Screen.REGISTER1 -> registrationOrigin
            Screen.REGISTER2 -> Screen.REGISTER1
            Screen.REGISTER3 -> Screen.REGISTER2
            Screen.RESET -> Screen.LOGIN
            Screen.SUCCESS -> Screen.DONE
            else -> Screen.WELCOME
        })
    }
    fun dismissMessage() { _ui.value = _ui.value.copy(message = null) }
    fun message(text: String) { _ui.value = _ui.value.copy(message = text) }
    fun next() {
        if (_ui.value.busy) return
        when (_ui.value.screen) {
            Screen.REGISTER1 -> AuthValidation.step1(draft)?.let(::message) ?: navigate(Screen.REGISTER2)
            Screen.REGISTER2 -> AuthValidation.step2(draft)?.let(::message) ?: navigate(Screen.REGISTER3)
            Screen.REGISTER3 -> {
                val error = AuthValidation.step3(draft)
                if (error != null) message(error) else execute {
                    useCases.register(draft)
                    draft = RegistrationDraft(); loginPassword = ""
                    Screen.SUCCESS
                }
            }
            Screen.SUCCESS -> navigate(Screen.DONE)
            else -> Unit
        }
    }
    fun login() = execute {
        if (useCases.login(loginEmail, loginPassword)) {
            draft = RegistrationDraft(); loginPassword = ""; Screen.DONE
        } else {
            // An account may exist after an interrupted upload; allow completing its profile.
            draft = draft.copy(email = loginEmail.trim(), password = loginPassword, confirm = loginPassword)
            registrationOrigin = Screen.LOGIN
            Screen.REGISTER1
        }
    }
    fun google() = execute { useCases.startGoogle(); Screen.LOGIN }
    fun handleCallback(code: String?, recovery: Boolean, error: Boolean) {
        if (error || code.isNullOrBlank()) { navigate(Screen.LOGIN); message("Не удалось завершить авторизацию. Попробуйте снова."); return }
        execute {
            val complete = useCases.finishGoogle(code)
            if (recovery) Screen.RESET else if (complete) Screen.DONE else {
                message("Профиль не завершён. Войдите с паролем и продолжите регистрацию.")
                Screen.LOGIN
            }
        }
    }
    fun sendReset() = execute {
        useCases.resetPassword(loginEmail)
        message("Если аккаунт существует, инструкция отправлена на почту.")
        Screen.LOGIN
    }
    fun changePassword() = execute {
        require(resetPassword.length >= 6) { "Пароль должен содержать не менее 6 символов." }
        require(resetPassword == resetConfirm) { "Пароли не совпадают." }
        val complete = useCases.updatePassword(resetPassword)
        resetPassword = ""; resetConfirm = ""
        if (complete) Screen.DONE else Screen.LOGIN
    }
    fun consumeDone() {
        draft = RegistrationDraft(); loginPassword = ""; resetPassword = ""; resetConfirm = ""
        _ui.value = Ui()
    }
    fun photo(uri: String) {
        draft = when (photoTarget) {
            "avatar" -> draft.copy(avatar = uri)
            "license" -> draft.copy(licensePhoto = uri)
            "passport" -> draft.copy(passportPhoto = uri)
            else -> draft
        }
        pendingPhoto = null
    }
    private fun execute(block: suspend () -> Screen) {
        if (_ui.value.busy) return
        _ui.value = _ui.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                val target = block()
                _ui.value = _ui.value.copy(screen = target, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                _ui.value = _ui.value.copy(busy = false, message = e.message ?: "Проверьте данные.")
            } catch (_: Exception) {
                _ui.value = _ui.value.copy(busy = false, message = "Не удалось выполнить запрос. Проверьте данные, интернет и настройки Supabase. Повторите попытку.")
            }
        }
    }
    class Factory(context: Context) : ViewModelProvider.Factory {
        private val app = context.applicationContext
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AccountViewModel::class.java))
            return AccountViewModel(AccountUseCases(AccountRepositoryImpl(SupabaseProvider.client, app))) as T
        }
    }
}
