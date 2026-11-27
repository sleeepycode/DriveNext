package ru.mtuci.drivenext

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.core.network.NetworkObserver
import ru.mtuci.drivenext.databinding.ActivityMainBinding
import ru.mtuci.drivenext.presentation.auth.AuthFragment
import ru.mtuci.drivenext.presentation.connection.NoConnectionFragment
import ru.mtuci.drivenext.presentation.connection.ServerErrorFragment
import ru.mtuci.drivenext.presentation.main.HomeFragment
import ru.mtuci.drivenext.presentation.onboarding.OnboardingFragment
import ru.mtuci.drivenext.presentation.splash.SplashFragment
import ru.mtuci.drivenext.presentation.splash.StartupState
import ru.mtuci.drivenext.presentation.splash.StartupViewModel
import ru.mtuci.drivenext.presentation.splash.StartupViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: StartupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(
            window,
            binding.root
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        viewModel = ViewModelProvider(
            this,
            StartupViewModelFactory(applicationContext)
        )[StartupViewModel::class.java]

        observeStartupState()
        observeNetwork()
    }

    private fun observeStartupState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.RESUMED) {

                viewModel.state.collect { state ->

                    Log.d("DriveNext", "Startup state: $state")

                    val fragment = when (state) {

                        StartupState.LOADING ->
                            SplashFragment()

                        StartupState.NO_CONNECTION ->
                            NoConnectionFragment()

                        StartupState.ONBOARDING ->
                            OnboardingFragment()

                        StartupState.AUTH ->
                            AuthFragment()

                        StartupState.MAIN ->
                            HomeFragment()

                        StartupState.SERVER_ERROR ->
                            ServerErrorFragment()
                    }

                    val currentFragment =
                        supportFragmentManager
                            .findFragmentById(R.id.main)

                    if (
                        currentFragment?.javaClass !=
                        fragment.javaClass
                    ) {

                        supportFragmentManager
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.main, fragment)
                            .commit()
                    }
                }
            }
        }
    }

    private fun observeNetwork() {

        val networkObserver =
            NetworkObserver(applicationContext)

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                networkObserver.observe().collect { isAvailable ->

                    Log.d(
                        "DriveNext",
                        "Internet available: $isAvailable"
                    )

                    viewModel.onNetworkChanged(isAvailable)
                }
            }
        }
    }
}