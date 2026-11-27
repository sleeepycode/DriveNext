package ru.mtuci.drivenext.domain.usecase

import ru.mtuci.drivenext.domain.repository.OnboardingRepository

class CompleteOnboardingUseCase(
    private val repository: OnboardingRepository
) {
    operator fun invoke() {
        repository.completeOnboarding()
    }
}