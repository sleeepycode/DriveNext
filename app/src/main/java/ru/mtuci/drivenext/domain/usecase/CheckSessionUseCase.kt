package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.model.SessionState
import ru.mtuci.drivenext.domain.repository.AuthRepository

class CheckSessionUseCase(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(): SessionState {
        return repository.checkSession()
    }
}