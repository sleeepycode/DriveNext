package ru.mtuci.drivenext.data.repository

import ru.mtuci.drivenext.data.local.OnboardingPreferences
import ru.mtuci.drivenext.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val preferences: OnboardingPreferences
) : OnboardingRepository {

    override fun isOnboardingCompleted(): Boolean {
        return preferences.isCompleted()
    }

    override fun completeOnboarding() {
        preferences.setCompleted()
    }
}