package ru.mtuci.drivenext.presentation.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.mtuci.drivenext.core.network.NetworkChecker
import ru.mtuci.drivenext.data.local.OnboardingPreferences
import ru.mtuci.drivenext.data.remote.SupabaseProvider
import ru.mtuci.drivenext.data.repository.AuthRepositoryImpl
import ru.mtuci.drivenext.data.repository.NetworkRepositoryImpl
import ru.mtuci.drivenext.data.repository.OnboardingRepositoryImpl
import ru.mtuci.drivenext.domain.usecase.CheckInternetUseCase
import ru.mtuci.drivenext.domain.usecase.CheckOnboardingUseCase
import ru.mtuci.drivenext.domain.usecase.CompleteOnboardingUseCase
import ru.mtuci.drivenext.domain.usecase.CheckSessionUseCase

class StartupViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                StartupViewModel::class.java
            )
        ) {

            val networkRepository = NetworkRepositoryImpl(
                NetworkChecker(appContext)
            )

            val onboardingRepository = OnboardingRepositoryImpl(
                OnboardingPreferences(appContext)
            )

            val authRepository = AuthRepositoryImpl(
                SupabaseProvider.client
            )

            return StartupViewModel(
                CheckInternetUseCase(networkRepository),
                CheckOnboardingUseCase(onboardingRepository),
                CompleteOnboardingUseCase(onboardingRepository),
                CheckSessionUseCase(authRepository)
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}