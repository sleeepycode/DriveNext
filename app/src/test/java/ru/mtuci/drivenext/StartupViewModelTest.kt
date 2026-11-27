package ru.mtuci.drivenext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import ru.mtuci.drivenext.domain.model.SessionState
import ru.mtuci.drivenext.domain.repository.AuthRepository
import ru.mtuci.drivenext.domain.repository.NetworkRepository
import ru.mtuci.drivenext.domain.repository.OnboardingRepository
import ru.mtuci.drivenext.domain.usecase.CheckInternetUseCase
import ru.mtuci.drivenext.domain.usecase.CheckOnboardingUseCase
import ru.mtuci.drivenext.domain.usecase.CheckSessionUseCase
import ru.mtuci.drivenext.domain.usecase.CompleteOnboardingUseCase
import ru.mtuci.drivenext.presentation.splash.StartupState
import ru.mtuci.drivenext.presentation.splash.StartupViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    private class FakeNetworkRepository : NetworkRepository {

        var available = true

        override fun isInternetAvailable(): Boolean {
            return available
        }
    }

    private class FakeOnboardingRepository :
        OnboardingRepository {

        var completed = false

        override fun isOnboardingCompleted(): Boolean {
            return completed
        }

        override fun completeOnboarding() {
            completed = true
        }
    }

    private class FakeAuthRepository : AuthRepository {

        var result = SessionState.NOT_AUTHENTICATED

        override suspend fun checkSession(): SessionState {
            return result
        }
    }

    private lateinit var network: FakeNetworkRepository
    private lateinit var onboarding: FakeOnboardingRepository
    private lateinit var auth: FakeAuthRepository

    @Before
    fun setup() {

        Dispatchers.setMain(StandardTestDispatcher())

        network = FakeNetworkRepository()
        onboarding = FakeOnboardingRepository()
        auth = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StartupViewModel {

        return StartupViewModel(
            CheckInternetUseCase(network),
            CheckOnboardingUseCase(onboarding),
            CompleteOnboardingUseCase(onboarding),
            CheckSessionUseCase(auth)
        )
    }

    @Test
    fun firstLaunchShowsOnboarding() = runTest {

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(
            StartupState.ONBOARDING,
            viewModel.state.value
        )
    }

    @Test
    fun offlineLaunchShowsNoConnection() = runTest {

        network.available = false

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(
            StartupState.NO_CONNECTION,
            viewModel.state.value
        )
    }

    @Test
    fun completedOnboardingShowsAuth() = runTest {

        onboarding.completed = true

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(
            StartupState.AUTH,
            viewModel.state.value
        )
    }

    @Test
    fun validSessionShowsMainScreen() = runTest {

        onboarding.completed = true
        auth.result = SessionState.AUTHENTICATED

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(
            StartupState.MAIN,
            viewModel.state.value
        )
    }

    @Test
    fun unavailableServerShowsError() = runTest {

        onboarding.completed = true
        auth.result = SessionState.UNAVAILABLE

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(
            StartupState.SERVER_ERROR,
            viewModel.state.value
        )
    }

    @Test
    fun finishingOnboardingSavesState() = runTest {

        val viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.finishOnboarding()

        advanceUntilIdle()

        assertTrue(onboarding.completed)

        assertEquals(
            StartupState.AUTH,
            viewModel.state.value
        )
    }

    @Test
    fun restoringInternetContinuesStartup() = runTest {

        network.available = false

        val viewModel = createViewModel()

        advanceUntilIdle()

        network.available = true

        viewModel.onNetworkChanged(true)

        advanceUntilIdle()

        assertEquals(
            StartupState.ONBOARDING,
            viewModel.state.value
        )
    }

    @Test
    fun losingInternetShowsNoConnection() = runTest {

        onboarding.completed = true

        val viewModel = createViewModel()

        advanceUntilIdle()

        network.available = false

        viewModel.onNetworkChanged(false)

        advanceUntilIdle()

        assertEquals(
            StartupState.NO_CONNECTION,
            viewModel.state.value
        )
    }

    @Test
    fun retryWithoutInternetKeepsErrorScreen() = runTest {

        network.available = false

        val viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.retryConnection()

        advanceUntilIdle()

        assertEquals(
            StartupState.NO_CONNECTION,
            viewModel.state.value
        )
    }

    @Test
    fun onboardingPageSurvivesNetworkLoss() = runTest {

        val viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.onboardingPage = 1

        network.available = false

        viewModel.onNetworkChanged(false)

        advanceUntilIdle()

        network.available = true

        viewModel.onNetworkChanged(true)

        advanceUntilIdle()

        assertEquals(1, viewModel.onboardingPage)

        assertEquals(
            StartupState.ONBOARDING,
            viewModel.state.value
        )
    }
}