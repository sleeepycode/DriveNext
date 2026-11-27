package ru.mtuci.drivenext.presentation.auth

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mtuci.drivenext.R
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AuthFragment : Fragment(R.layout.fragment_account_host) {
    private lateinit var vm: AccountViewModel
    private var content: FrameLayout? = null
    private var loading: View? = null
    private var displayed: AccountViewModel.Screen? = null
    private var dialog: AlertDialog? = null
    private var backCallback: OnBackPressedCallback? = null

    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            vm.photo(uri.toString())
            content?.let { refreshPhotos(it); updateEnabled(it) }
        }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = vm.pendingPhoto
        if (ok && uri != null) vm.photo(uri) else vm.pendingPhoto = null
        content?.let { refreshPhotos(it); updateEnabled(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity(), AccountViewModel.Factory(requireContext()))[AccountViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content = view.findViewById(R.id.accountContent)
        loading = view.findViewById(R.id.accountLoading)
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { hideKeyboard(); vm.back() }
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { state ->
                    if (state.screen != displayed) render(state.screen)
                    loading?.visibility = if (state.busy) View.VISIBLE else View.GONE
                    backCallback?.isEnabled = state.screen != AccountViewModel.Screen.WELCOME || state.busy
                    content?.let { updateEnabled(it) }
                    state.message?.let { message ->
                        if (dialog?.isShowing != true) {
                            dialog = AlertDialog.Builder(requireContext())
                                .setTitle("DriveNext").setMessage(message)
                                .setPositiveButton("ОК", null).create().also { alert ->
                                    alert.setOnDismissListener { vm.dismissMessage(); dialog = null }
                                    alert.show()
                                }
                        }
                    }
                }
            }
        }
    }

    private fun render(screen: AccountViewModel.Screen) {
        val container = content ?: return
        if (screen == AccountViewModel.Screen.DONE) return
        displayed = screen
        val layout = when (screen) {
            AccountViewModel.Screen.WELCOME -> R.layout.fragment_auth
            AccountViewModel.Screen.LOGIN -> R.layout.fragment_login
            AccountViewModel.Screen.REGISTER1 -> R.layout.fragment_register_one
            AccountViewModel.Screen.REGISTER2 -> R.layout.fragment_register_two
            AccountViewModel.Screen.REGISTER3 -> R.layout.fragment_register_three
            AccountViewModel.Screen.SUCCESS -> R.layout.fragment_register_success
            AccountViewModel.Screen.RESET -> R.layout.fragment_reset_password
            AccountViewModel.Screen.DONE -> return
        }
        container.removeAllViews()
        val form = layoutInflater.inflate(layout, container, false)
        container.addView(form)
        form.alpha = 0f
        form.animate().alpha(1f).setDuration(180).start()
        form.findViewById<View>(R.id.backButton)?.setOnClickListener { hideKeyboard(); vm.back() }
        when (screen) {
            AccountViewModel.Screen.WELCOME -> {
                form.findViewById<View>(R.id.loginButton).setOnClickListener { vm.navigate(AccountViewModel.Screen.LOGIN) }
                form.findViewById<View>(R.id.registerButton).setOnClickListener { vm.startRegistration() }
            }
            AccountViewModel.Screen.LOGIN -> {
                field(form, R.id.loginEmail, vm.loginEmail) { vm.loginEmail = it }
                field(form, R.id.loginPassword, vm.loginPassword) { vm.loginPassword = it }
                form.findViewById<View>(R.id.submitButton).setOnClickListener { hideKeyboard(); vm.login() }
                form.findViewById<View>(R.id.googleButton).setOnClickListener { hideKeyboard(); vm.google() }
                form.findViewById<View>(R.id.forgotButton).setOnClickListener { hideKeyboard(); vm.sendReset() }
                form.findViewById<View>(R.id.toRegisterButton).setOnClickListener { hideKeyboard(); vm.startRegistration() }
            }
            AccountViewModel.Screen.REGISTER1 -> {
                field(form, R.id.emailField, vm.draft.email) { vm.draft = vm.draft.copy(email = it) }
                field(form, R.id.passwordField, vm.draft.password) { vm.draft = vm.draft.copy(password = it) }
                field(form, R.id.confirmField, vm.draft.confirm) { vm.draft = vm.draft.copy(confirm = it) }
                form.findViewById<CheckBox>(R.id.consentField).apply {
                    isChecked = vm.draft.consent
                    setOnCheckedChangeListener { _, checked -> vm.draft = vm.draft.copy(consent = checked) }
                }
                form.findViewById<View>(R.id.privacyButton).setOnClickListener {
                    vm.message("Учебный проект DriveNext. Вводите только вымышленные персональные данные и учебные изображения. Данные используются для демонстрации регистрации и хранятся в закрытом хранилище Supabase. Не загружайте настоящие паспорта и водительские удостоверения.")
                }
            }
            AccountViewModel.Screen.REGISTER2 -> {
                field(form, R.id.lastNameField, vm.draft.lastName) { vm.draft = vm.draft.copy(lastName = it) }
                field(form, R.id.firstNameField, vm.draft.firstName) { vm.draft = vm.draft.copy(firstName = it) }
                field(form, R.id.middleNameField, vm.draft.middleName) { vm.draft = vm.draft.copy(middleName = it) }
                date(form, R.id.birthField, R.id.birthFieldLayout, vm.draft.birth, "MM/dd/uuuu") { vm.draft = vm.draft.copy(birth = it) }
                form.findViewById<RadioGroup>(R.id.genderField).apply {
                    if (vm.draft.gender == "male") check(R.id.maleField)
                    if (vm.draft.gender == "female") check(R.id.femaleField)
                    setOnCheckedChangeListener { _, id ->
                        vm.draft = vm.draft.copy(gender = when(id) { R.id.maleField -> "male"; R.id.femaleField -> "female"; else -> "" })
                        updateEnabled(form)
                    }
                }
            }
            AccountViewModel.Screen.REGISTER3 -> {
                field(form, R.id.licenseField, vm.draft.license) { vm.draft = vm.draft.copy(license = it) }
                date(form, R.id.issuedField, R.id.issuedFieldLayout, vm.draft.issued, "dd/MM/uuuu") { vm.draft = vm.draft.copy(issued = it) }
                form.findViewById<View>(R.id.avatarPreview).setOnClickListener { choosePhoto("avatar") }
                form.findViewById<View>(R.id.licenseButton).setOnClickListener { choosePhoto("license") }
                form.findViewById<View>(R.id.passportButton).setOnClickListener { choosePhoto("passport") }
                refreshPhotos(form)
            }
            AccountViewModel.Screen.RESET -> {
                field(form, R.id.newPassword, vm.resetPassword) { vm.resetPassword = it }
                field(form, R.id.newConfirm, vm.resetConfirm) { vm.resetConfirm = it }
            }
            else -> Unit
        }
        form.findViewById<View>(R.id.nextButton)?.setOnClickListener {
            hideKeyboard()
            if (screen == AccountViewModel.Screen.RESET) vm.changePassword() else vm.next()
        }
        updateEnabled(form)
    }

    private fun field(form: View, id: Int, value: String, update: (String) -> Unit) {
        val edit = form.findViewById<EditText>(id)
        edit.setText(value)
        edit.doAfterTextChanged { update(it.toString()); updateEnabled(form) }
    }

    private fun date(form: View, id: Int, layoutId: Int, value: String, pattern: String, update: (String) -> Unit) {
        val edit = form.findViewById<EditText>(id)
        edit.setText(value)
        fun open() {
            if (parentFragmentManager.findFragmentByTag("lab2_date_$id") != null) return
            val today = MaterialDatePicker.todayInUtcMilliseconds()
            val formatter = DateTimeFormatter.ofPattern(pattern)
            val start = LocalDate.of(1900, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val initial = try { LocalDate.parse(edit.text.toString(), formatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() } catch (_: Exception) { today }
            val constraints = CalendarConstraints.Builder().setStart(start).setEnd(today)
                .setValidator(DateValidatorPointBackward.now()).build()
            MaterialDatePicker.Builder.datePicker().setTitleText("Выбор даты")
                .setSelection(initial).setCalendarConstraints(constraints).build().apply {
                    addOnPositiveButtonClickListener { millis ->
                        val text = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
                        edit.setText(text); update(text); updateEnabled(form)
                    }
                    show(this@AuthFragment.parentFragmentManager, "lab2_date_$id")
                }
        }
        edit.setOnClickListener { open() }
        form.findViewById<TextInputLayout>(layoutId).setEndIconOnClickListener { open() }
    }

    private fun updateEnabled(form: View) {
        val d = vm.draft
        val busy = vm.ui.value.busy
        fun enable(id: Int, enabled: Boolean) {
            form.findViewById<Button>(id)?.apply { isEnabled = enabled && !busy; alpha = if (isEnabled) 1f else 0.45f }
        }
        when (displayed) {
            AccountViewModel.Screen.LOGIN -> enable(R.id.submitButton, vm.loginEmail.isNotBlank() && vm.loginPassword.isNotBlank())
            AccountViewModel.Screen.REGISTER2 -> enable(R.id.nextButton, d.lastName.isNotBlank() && d.firstName.isNotBlank() && d.birth.isNotBlank() && d.gender.isNotBlank())
            AccountViewModel.Screen.REGISTER3 -> enable(R.id.nextButton, d.license.isNotBlank() && d.issued.isNotBlank() && d.licensePhoto != null && d.passportPhoto != null)
            AccountViewModel.Screen.RESET -> enable(R.id.nextButton, vm.resetPassword.isNotBlank() && vm.resetConfirm.isNotBlank())
            AccountViewModel.Screen.REGISTER1, AccountViewModel.Screen.SUCCESS -> enable(R.id.nextButton, true)
            else -> Unit
        }
    }

    private fun choosePhoto(target: String) {
        vm.photoTarget = target
        dialog = AlertDialog.Builder(requireContext()).setTitle("Добавить фотографию")
            .setItems(arrayOf("Галерея", "Камера")) { _, which ->
                try {
                    if (which == 0) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    else {
                        val context = requireContext()
                        val directory = File(context.cacheDir, "registration_photos").apply { mkdirs() }
                        val file = File.createTempFile("photo_", ".jpg", directory)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        vm.pendingPhoto = uri.toString()
                        camera.launch(uri)
                    }
                } catch (_: Exception) { vm.message("Не удалось открыть камеру или галерею.") }
            }.create().also { it.setOnDismissListener { dialog = null }; it.show() }
    }

    private fun refreshPhotos(form: View) {
        if (displayed != AccountViewModel.Screen.REGISTER3) return
        val resolver = requireContext().contentResolver
        fun preview(id: Int, uri: String?) {
            val image = form.findViewById<ImageView>(id) ?: return
            if (uri == null) return
            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                    try { resolver.loadThumbnail(Uri.parse(uri), Size(240, 240), null) }
                    catch (_: Exception) { null }
                }
                if (bitmap != null) image.setImageBitmap(bitmap) else image.setImageResource(R.drawable.ic_lab2_success)
            }
        }
        preview(R.id.avatarPreview, vm.draft.avatar)
        preview(R.id.licensePreview, vm.draft.licensePhoto)
        preview(R.id.passportPreview, vm.draft.passportPhoto)
        form.findViewById<Button>(R.id.licenseButton)?.text = if (vm.draft.licensePhoto == null) "Загрузить фото" else "Заменить фото"
        form.findViewById<Button>(R.id.passportButton)?.text = if (vm.draft.passportPhoto == null) "Загрузить фото" else "Заменить фото"
    }

    private fun hideKeyboard() {
        view?.let { v -> (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0) }
    }
    override fun onDestroyView() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss(); dialog = null
        content = null; loading = null; displayed = null; backCallback = null
        super.onDestroyView()
    }
}
