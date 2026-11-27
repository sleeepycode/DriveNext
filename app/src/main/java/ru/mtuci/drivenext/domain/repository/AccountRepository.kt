package ru.mtuci.drivenext.domain.repository

import ru.mtuci.drivenext.domain.model.RegistrationDraft

interface AccountRepository {
    suspend fun login(email: String, password: String): Boolean
    suspend fun register(draft: RegistrationDraft)
    suspend fun startGoogle()
    suspend fun finishGoogle(code: String): Boolean
    suspend fun resetPassword(email: String)
    suspend fun updatePassword(password: String): Boolean
}
