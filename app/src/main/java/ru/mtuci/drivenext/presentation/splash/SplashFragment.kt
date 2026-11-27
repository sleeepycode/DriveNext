package ru.mtuci.drivenext.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentSplashBinding

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSplashBinding.bind(view)

        binding.splashTitle.apply {
            alpha = 0f
            translationY = -20f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
        }

        binding.splashSubtitle.apply {
            alpha = 0f

            animate()
                .alpha(1f)
                .setStartDelay(200)
                .setDuration(700)
                .start()
        }

        binding.splashImage.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(150)
                .setDuration(900)
                .start()
        }
    }
}