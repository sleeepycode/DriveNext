package ru.mtuci.drivenext.presentation.onboarding

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentOnboardingBinding
import ru.mtuci.drivenext.presentation.splash.StartupViewModel

class OnboardingFragment :
    Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null

    private val binding
        get() = requireNotNull(_binding)

    private lateinit var viewModel: StartupViewModel

    private val indicatorAnimators =
        mutableListOf<ValueAnimator>()

    private val pages = listOf(
        OnboardingPage(
            R.drawable.share,
            R.string.onboarding_title_1,
            R.string.onboarding_description_1
        ),
        OnboardingPage(
            R.drawable.safe,
            R.string.onboarding_title_2,
            R.string.onboarding_description_2
        ),
        OnboardingPage(
            R.drawable.best,
            R.string.onboarding_title_3,
            R.string.onboarding_description_3
        )
    )

    private val pageCallback =
        object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {

                super.onPageSelected(position)

                if (_binding != null) {

                    viewModel.onboardingPage = position

                    updateIndicators(position)
                    updateButton(position)
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOnboardingBinding.bind(view)

        viewModel = ViewModelProvider(
            requireActivity()
        )[StartupViewModel::class.java]

        setupPager()
        setupButtons()
    }

    private fun setupPager() {

        binding.onboardingPager.adapter =
            OnboardingAdapter(pages)

        binding.onboardingPager.offscreenPageLimit = 1

        val savedPage = viewModel.onboardingPage
            .coerceIn(0, pages.lastIndex)

        binding.onboardingPager.setCurrentItem(
            savedPage,
            false
        )

        binding.onboardingPager.registerOnPageChangeCallback(
            pageCallback
        )

        binding.onboardingPager.setPageTransformer { page, position ->

            val image = page.findViewById<View>(
                R.id.onboardingImage
            )

            val progress =
                kotlin.math.abs(position).coerceAtMost(1f)

            image.translationX =
                -position * page.width * 0.15f

            image.scaleX = 1f - progress * 0.05f
            image.scaleY = 1f - progress * 0.05f

            image.alpha = 1f - progress * 0.2f
        }

        updateIndicators(savedPage)
        updateButton(savedPage)
    }

    private fun setupButtons() {

        binding.skipButton.setOnClickListener {

            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY
            )

            viewModel.finishOnboarding()
        }

        binding.nextButton.setOnClickListener {

            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY
            )

            val currentPage =
                binding.onboardingPager.currentItem

            if (currentPage < pages.lastIndex) {

                binding.onboardingPager.setCurrentItem(
                    currentPage + 1,
                    true
                )

            } else {

                viewModel.finishOnboarding()
            }
        }
    }

    private fun updateButton(position: Int) {

        val textRes = if (position == pages.lastIndex) {
            R.string.onboarding_finish
        } else {
            R.string.onboarding_next
        }

        binding.nextButton.setText(textRes)
    }

    private fun updateIndicators(position: Int) {

        indicatorAnimators.forEach { it.cancel() }
        indicatorAnimators.clear()

        val indicators = listOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3
        )

        indicators.forEachIndexed { index, indicator ->

            val isActive = index == position

            val targetWidth =
                resources.getDimensionPixelSize(
                    if (isActive) {
                        R.dimen.indicator_active_width
                    } else {
                        R.dimen.indicator_inactive_width
                    }
                )

            val color = requireContext().getColor(
                if (isActive) {
                    R.color.indicator_active
                } else {
                    R.color.indicator_inactive
                }
            )

            indicator.backgroundTintList =
                ColorStateList.valueOf(color)

            val animator = ValueAnimator.ofInt(
                indicator.layoutParams.width,
                targetWidth
            ).apply {

                duration = 250

                interpolator = DecelerateInterpolator()

                addUpdateListener { animation ->

                    indicator.layoutParams =
                        indicator.layoutParams.apply {
                            width = animation.animatedValue as Int
                        }
                }
            }

            indicatorAnimators.add(animator)
            animator.start()
        }
    }

    override fun onDestroyView() {

        binding.onboardingPager.unregisterOnPageChangeCallback(
            pageCallback
        )

        indicatorAnimators.forEach { it.cancel() }
        indicatorAnimators.clear()

        _binding = null

        super.onDestroyView()
    }
}