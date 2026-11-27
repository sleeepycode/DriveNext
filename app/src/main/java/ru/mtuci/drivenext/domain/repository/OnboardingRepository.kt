package ru.mtuci.drivenext.domain.repository

interface OnboardingRepository {

    fun isOnboardingCompleted(): Boolean

    fun completeOnboarding()
}