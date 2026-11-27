package ru.mtuci.drivenext.data.local

import android.content.Context

class OnboardingPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        "drivenext_preferences",
        Context.MODE_PRIVATE
    )

    fun isCompleted(): Boolean {
        return preferences.getBoolean(
            "onboarding_completed",
            false
        )
    }

    fun setCompleted() {
        preferences.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
    }
}