package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.repository.NetworkRepository

class CheckInternetUseCase(
    private val repository: NetworkRepository
) {
    operator fun invoke(): Boolean {
        return repository.isInternetAvailable()
    }
}