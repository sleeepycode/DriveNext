package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.repository.OnboardingRepository

class CheckOnboardingUseCase(
    private val repository: OnboardingRepository
) {
    operator fun invoke(): Boolean {
        return repository.isOnboardingCompleted()
    }
}