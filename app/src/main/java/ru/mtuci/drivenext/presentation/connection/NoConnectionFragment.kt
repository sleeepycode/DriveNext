package ru.mtuci.drivenext.presentation.connection

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentNoConnectionBinding
import ru.mtuci.drivenext.presentation.splash.StartupViewModel

class NoConnectionFragment :
    Fragment(R.layout.fragment_no_connection) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        val binding =
            FragmentNoConnectionBinding.bind(view)

        val viewModel = ViewModelProvider(
            requireActivity()
        )[StartupViewModel::class.java]

        binding.retryButton.setOnClickListener {
            viewModel.retryConnection()
        }
    }
}