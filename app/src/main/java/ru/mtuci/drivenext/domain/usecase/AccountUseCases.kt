package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.model.AuthValidation
import ru.mtuci.drivenext.domain.model.RegistrationDraft
import ru.mtuci.drivenext.domain.repository.AccountRepository

class AccountUseCases(private val repository: AccountRepository) {
    suspend fun login(email: String, password: String): Boolean {
        require(AuthValidation.email(email)) { "Введите корректный адрес электронной почты." }
        require(password.isNotBlank()) { "Введите пароль." }
        return repository.login(email.trim(), password)
    }
    suspend fun register(draft: RegistrationDraft) {
        val error = AuthValidation.step1(draft) ?: AuthValidation.step2(draft) ?: AuthValidation.step3(draft)
        require(error == null) { error.orEmpty() }
        repository.register(draft)
    }
    suspend fun startGoogle() = repository.startGoogle()
    suspend fun finishGoogle(code: String) = repository.finishGoogle(code)
    suspend fun resetPassword(email: String) {
        require(AuthValidation.email(email)) { "Введите корректный адрес электронной почты." }
        repository.resetPassword(email.trim())
    }
    suspend fun updatePassword(password: String) = repository.updatePassword(password)
}
