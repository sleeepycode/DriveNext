package ru.mtuci.drivenext.presentation.main

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentHomeBinding
import ru.mtuci.drivenext.databinding.FragmentCatalogBinding
import ru.mtuci.drivenext.databinding.FragmentSettingsBinding
import ru.mtuci.drivenext.databinding.FragmentProfileBinding
import ru.mtuci.drivenext.databinding.FragmentCatalogDestinationBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var binding: FragmentHomeBinding? = null
    private lateinit var vm: CatalogViewModel
    private var shown: CatalogViewModel.Screen? = null
    private var catalog: FragmentCatalogBinding? = null
    private var settings: FragmentSettingsBinding? = null
    private var profile: FragmentProfileBinding? = null
    private var destination: FragmentCatalogDestinationBinding? = null
    private var adapter: CarAdapter? = null
    private var updatingSearch = false
    private var updatingNav = false
    private var dialog: AlertDialog? = null
    private var renderedAvatar: ByteArray? = null
    private var displayedError: String? = null
    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && ::vm.isInitialized) vm.updateAvatar(uri.toString())
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentHomeBinding.bind(view)
        binding = b
        vm = ViewModelProvider(requireActivity(), CatalogViewModel.Factory(requireContext()))[CatalogViewModel::class.java]
        b.bottomNavigation.setOnItemSelectedListener {
            if (!updatingNav) when (it.itemId) {
                R.id.lab3_home -> { hideKeyboard(); catalog?.carSearch?.clearFocus(); vm.home() }
                R.id.lab3_saved -> vm.saved()
                R.id.lab3_settings -> vm.settings()
            }
            true
        }
        b.retryCatalog.setOnClickListener { vm.retry() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!vm.back()) { isEnabled = false; requireActivity().onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        })
        vm.open()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { render(it) }
            }
        }
    }
    private fun render(ui: CatalogViewModel.Ui) {
        val b = binding ?: return
        if (ui.signedOut) return
        val screen = ui.screen
        val previous = shown
        val list = screen == CatalogViewModel.Screen.HOME || screen == CatalogViewModel.Screen.SEARCH
        val previousList = previous == CatalogViewModel.Screen.HOME || previous == CatalogViewModel.Screen.SEARCH
        if (previous != screen && !(list && previousList)) {
            b.catalogContent.removeAllViews()
            catalog = null; settings = null; profile = null; destination = null; adapter = null; renderedAvatar = null
            when (screen) {
                CatalogViewModel.Screen.HOME, CatalogViewModel.Screen.SEARCH -> buildCatalog(b)
                CatalogViewModel.Screen.SETTINGS -> buildSettings(b)
                CatalogViewModel.Screen.PROFILE -> buildProfile(b)
                else -> buildDestination(b)
            }
        }
        shown = screen
        updatingNav = true
        val tab = when (screen) {
            CatalogViewModel.Screen.SETTINGS, CatalogViewModel.Screen.PROFILE, CatalogViewModel.Screen.BOOKINGS, CatalogViewModel.Screen.ADD_CAR -> R.id.lab3_settings
            CatalogViewModel.Screen.SAVED -> R.id.lab3_saved
            else -> R.id.lab3_home
        }
        if (b.bottomNavigation.selectedItemId != tab) b.bottomNavigation.selectedItemId = tab
        updatingNav = false
        b.bottomNavigation.menu.setGroupEnabled(0, !ui.busy || list)
        b.catalogProgress.isVisible = ui.busy
        b.catalogError.isVisible = ui.error != null
        b.errorText.text = ui.error.orEmpty()
        catalog?.let { c ->
            val searching = screen == CatalogViewModel.Screen.SEARCH
            c.searchTitle.isVisible = searching
            c.homeTitle.isVisible = !searching
            c.searchBack.isVisible = searching
            // Keep the actual input (including trailing spaces) during typing.
            if (previous == null || !previousList || (!c.carSearch.hasFocus() && c.carSearch.query.toString() != ui.query)) {
                updatingSearch = true
                c.carSearch.setQuery(ui.query, false)
                updatingSearch = false
            }
            adapter?.submitList(ui.cars)
            c.emptyCars.isVisible = !ui.busy && ui.error == null && ui.cars.isEmpty()
            c.emptyCars.text = if (searching) "По вашему запросу ничего не найдено" else "Нет доступных автомобилей"
        }
        settings?.let { s ->
            s.settingsName.text = ui.profile?.displayName.orEmpty()
            s.settingsEmail.text = ui.profile?.email.orEmpty()
            s.settingsProfile.isEnabled = ui.profile != null && !ui.busy
            showAvatar(s.settingsAvatar, ui.avatar)
        }
        profile?.let { p ->
            val user = ui.profile
            p.profileName.text = user?.displayName.orEmpty()
            p.profileSince.text = user?.createdAt?.take(10)?.let { "Присоединился: $it" }.orEmpty()
            p.profileEmail.text = user?.email.orEmpty()
            p.profileGender.text = when (user?.gender) { "male" -> "Мужской"; "female" -> "Женский"; else -> "Не указан" }
            p.profileGoogle.text = if (user?.provider == "google") user.email else "Не подключён"
            p.profileAvatar.isEnabled = user != null && !ui.busy
            p.profileLogout.isEnabled = !ui.busy
            showAvatar(p.profileAvatar, ui.avatar)
        }
        destination?.let { d ->
            d.destinationTitle.text = when (screen) {
                CatalogViewModel.Screen.SAVED -> "Избранное"
                CatalogViewModel.Screen.BOOKINGS -> "Мои бронирования"
                CatalogViewModel.Screen.ADD_CAR -> "Подключить свой автомобиль"
                CatalogViewModel.Screen.RENT -> "Оформление аренды"
                else -> "Детали"
            }
            val car = ui.selected.takeIf { screen == CatalogViewModel.Screen.RENT || screen == CatalogViewModel.Screen.DETAILS }
            d.destinationCar.isVisible = car != null
            d.destinationImage.isVisible = car != null
            if (car != null) {
                d.destinationCar.text = "${car.brand} ${car.model}\n${car.dailyPrice} ₽ / день · ${car.transmission} · ${car.fuel}"
                d.destinationImage.setImageResource(CarAdapter.image(car.imageKey))
            }
            d.destinationMessage.text = if (screen == CatalogViewModel.Screen.ADD_CAR) "Экран следующей лабораторной работы." else "Функциональность будет реализована в лабораторной работе №4."
        }
        if (ui.error == null) displayedError = null
        if (ui.error != null && ui.error != displayedError && dialog?.isShowing != true) {
            displayedError = ui.error
            dialog = AlertDialog.Builder(requireContext()).setMessage(ui.error).setPositiveButton("ОК", null).create().also { it.show() }
        }
        if (ui.message != null && dialog?.isShowing != true) {
            dialog = AlertDialog.Builder(requireContext()).setMessage(ui.message).setPositiveButton("ОК", null)
                .setOnDismissListener { vm.dismissMessage() }.create().also { it.show() }
        }
    }
    private fun buildCatalog(b: FragmentHomeBinding) {
        val c = FragmentCatalogBinding.inflate(layoutInflater, b.catalogContent, true)
        catalog = c
        adapter = CarAdapter({ hideKeyboard(); vm.car(it, false) }, { hideKeyboard(); vm.car(it, true) })
        c.carList.layoutManager = LinearLayoutManager(requireContext())
        c.carList.adapter = adapter
        c.carSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { hideKeyboard(); vm.search(query.orEmpty(), true); return true }
            override fun onQueryTextChange(newText: String?): Boolean { if (!updatingSearch) vm.search(newText.orEmpty()); return true }
        })
        c.searchBack.setOnClickListener { hideKeyboard(); c.carSearch.clearFocus(); vm.home() }
        updatingSearch = true
        c.carSearch.setQuery(vm.ui.value.query, false)
        updatingSearch = false
        c.carSearch.clearFocus()
    }
    private fun buildSettings(b: FragmentHomeBinding) {
        hideKeyboard()
        val s = FragmentSettingsBinding.inflate(layoutInflater, b.catalogContent, true)
        settings = s
        s.settingsProfile.setOnClickListener { vm.profile() }
        s.settingsBookings.setOnClickListener { vm.bookings() }
        s.settingsAddCar.setOnClickListener { vm.addCar() }
        listOf(s.settingsTheme, s.settingsNotifications, s.settingsHelp, s.settingsInvite).forEach { row ->
            row.setOnClickListener { AlertDialog.Builder(requireContext()).setMessage("Этот пункт предусмотрен макетом. В ЛР №3 отдельная функциональность не требуется.").setPositiveButton("ОК", null).show() }
        }
    }
    private fun buildProfile(b: FragmentHomeBinding) {
        val p = FragmentProfileBinding.inflate(layoutInflater, b.catalogContent, true)
        profile = p
        p.profileBack.setOnClickListener { vm.back() }
        p.profileAvatar.setOnClickListener { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        p.profileLogout.setOnClickListener {
            AlertDialog.Builder(requireContext()).setMessage("Выйти из профиля?")
                .setNegativeButton("Отмена", null).setPositiveButton("Выйти") { _, _ -> vm.signOut() }.show()
        }
    }
    private fun buildDestination(b: FragmentHomeBinding) {
        destination = FragmentCatalogDestinationBinding.inflate(layoutInflater, b.catalogContent, true).also { d ->
            d.destinationBack.setOnClickListener { vm.back() }
        }
    }
    private fun showAvatar(view: ImageView, bytes: ByteArray?) {
        if (renderedAvatar === bytes && bytes != null) return
        renderedAvatar = bytes
        val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        if (bitmap != null) view.setImageBitmap(bitmap) else view.setImageResource(R.drawable.ic_lab2_avatar)
    }
    private fun hideKeyboard() {
        val v = view ?: return
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
    }
    override fun onDestroyView() {
        dialog?.dismiss(); dialog = null
        catalog?.carList?.adapter = null
        catalog = null; settings = null; profile = null; destination = null; adapter = null
        binding = null; shown = null; renderedAvatar = null; displayedError = null
        super.onDestroyView()
    }
}
