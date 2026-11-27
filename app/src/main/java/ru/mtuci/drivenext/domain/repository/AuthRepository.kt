package ru.mtuci.drivenext.domain.repository

import ru.mtuci.drivenext.domain.model.SessionState

interface AuthRepository {

    suspend fun checkSession(): SessionState
}