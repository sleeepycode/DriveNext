package ru.mtuci.drivenext.presentation.connection

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentNoConnectionBinding
import ru.mtuci.drivenext.presentation.splash.StartupViewModel

class ServerErrorFragment :
    Fragment(R.layout.fragment_no_connection) {

    private var errorDialog: AlertDialog? = null

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

        binding.connectionTitle.setText(
            R.string.server_error_title
        )

        binding.connectionDescription.setText(
            R.string.server_error_description
        )

        binding.connectionImage.visibility = View.GONE

        binding.retryButton.setOnClickListener {
            viewModel.retryConnection()
        }

        if (savedInstanceState == null) {

            errorDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.server_error_title)
                .setMessage(R.string.server_error_description)
                .setPositiveButton(R.string.dialog_retry) { _, _ ->
                    viewModel.retryConnection()
                }
                .setNegativeButton(R.string.dialog_close, null)
                .create()

            errorDialog?.show()
        }
    }

    override fun onDestroyView() {

        errorDialog?.dismiss()
        errorDialog = null

        super.onDestroyView()
    }
}