package ru.mtuci.drivenext.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.domain.model.SessionState
import ru.mtuci.drivenext.domain.usecase.CheckInternetUseCase
import ru.mtuci.drivenext.domain.usecase.CheckOnboardingUseCase
import ru.mtuci.drivenext.domain.usecase.CompleteOnboardingUseCase
import ru.mtuci.drivenext.domain.usecase.CheckSessionUseCase

class StartupViewModel(
    private val checkInternet: CheckInternetUseCase,
    private val checkOnboarding: CheckOnboardingUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val checkSession: CheckSessionUseCase
) : ViewModel() {

    private val _state =
        MutableStateFlow(StartupState.LOADING)

    val state = _state.asStateFlow()

    private var startupJob: Job? = null

    var onboardingPage: Int = 0
    private var resumeAuth = false

    init {
        checkStartupState(initialLaunch = true)
    }

    private fun checkStartupState(
        initialLaunch: Boolean = false
    ) {

        if (startupJob?.isActive == true) return

        startupJob = viewModelScope.launch {

            if (initialLaunch) {
                delay(2500)
            }

            if (!checkInternet()) {
                _state.value = StartupState.NO_CONNECTION
                return@launch
            }

            if (!checkOnboarding()) {
                _state.value = StartupState.ONBOARDING
                return@launch
            }

            _state.value = StartupState.LOADING

            val sessionState = checkSession()

            _state.value = when (sessionState) {

                SessionState.AUTHENTICATED ->
                    StartupState.MAIN

                SessionState.NOT_AUTHENTICATED ->
                    StartupState.AUTH

                SessionState.UNAVAILABLE ->
                    if (!checkInternet()) {
                        StartupState.NO_CONNECTION
                    } else {
                        StartupState.SERVER_ERROR
                    }
            }
        }
    }

    fun onNetworkChanged(isAvailable: Boolean) {

        if (!isAvailable) {
            if (_state.value == StartupState.AUTH) resumeAuth = true

            startupJob?.cancel()
            startupJob = null

            _state.value = StartupState.NO_CONNECTION

        } else if (
            _state.value == StartupState.NO_CONNECTION
        ) {

            if (resumeAuth) {
                resumeAuth = false
                _state.value = StartupState.AUTH
            } else checkStartupState()
        }
    }

    fun showAuthFlow() {
        startupJob?.cancel()
        startupJob = null
        if (checkInternet()) _state.value = StartupState.AUTH
        else { resumeAuth = true; _state.value = StartupState.NO_CONNECTION }
    }

    fun authenticated() {
        startupJob?.cancel()
        startupJob = null
        resumeAuth = false
        if (checkInternet()) _state.value = StartupState.MAIN
        else _state.value = StartupState.NO_CONNECTION
    }

    fun retryConnection() {
        if (resumeAuth && checkInternet()) {
            resumeAuth = false
            _state.value = StartupState.AUTH
        } else checkStartupState()
    }

    fun finishOnboarding() {
        completeOnboarding()
        onboardingPage = 0
        checkStartupState()
    }
}