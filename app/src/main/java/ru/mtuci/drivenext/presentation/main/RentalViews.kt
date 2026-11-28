package ru.mtuci.drivenext.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.*
import ru.mtuci.drivenext.domain.model.RentalRules
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Owns the view bindings of the six Lab 4 screens. The ViewModel owns state.
internal class RentalViews(private val inflater: LayoutInflater, private val parent: ViewGroup, private val vm: CatalogViewModel, screen: CatalogViewModel.Screen) {
    private val context = parent.context
    private var details: FragmentCarDetailsBinding? = null
    private var rent: FragmentRentalBinding? = null
    private var saved: FragmentSavedCarsBinding? = null
    private var bookings: FragmentBookingsBinding? = null
    private var booking: FragmentBookingDetailsBinding? = null
    private var carAdapter: CarAdapter? = null
    private var bookingAdapter: BookingAdapter? = null
    private var syncing = false
    init {
        when (screen) {
            CatalogViewModel.Screen.DETAILS -> details = FragmentCarDetailsBinding.inflate(inflater,parent,true).also { b ->
                b.rentalBack.setOnClickListener { vm.back() }; b.favoriteButton.setOnClickListener { vm.toggleFavorite() }; b.rentButton.setOnClickListener { vm.rentSelected() }
            }
            CatalogViewModel.Screen.RENT -> rent = FragmentRentalBinding.inflate(inflater,parent,true).also { b ->
                b.rentalBack.setOnClickListener { vm.back() }; b.continueRent.setOnClickListener { vm.submitRental() }
                b.startDate.setOnClickListener { vm.ui.value.rentalDraft?.let { d -> chooseDate(d.start) { vm.changeDates(start = it) } } }
                b.endDate.setOnClickListener { vm.ui.value.rentalDraft?.let { d -> chooseDate(d.end) { vm.changeDates(end = it) } } }
                b.driverName.imeHintLocales = android.os.LocaleList.forLanguageTags("ru-RU,en-US")
                b.driverName.doAfterTextChanged { if (!syncing) vm.changeDriver(name = it.toString()) }
                b.driverLicense.doAfterTextChanged { if (!syncing) vm.changeDriver(license = it.toString()) }
            }
            CatalogViewModel.Screen.SAVED -> saved = FragmentSavedCarsBinding.inflate(inflater,parent,true).also { b ->
                carAdapter = CarAdapter({ vm.car(it,false) }, { vm.car(it,true) }); b.savedList.layoutManager = LinearLayoutManager(context); b.savedList.adapter = carAdapter
            }
            CatalogViewModel.Screen.BOOKINGS -> bookings = FragmentBookingsBinding.inflate(inflater,parent,true).also { b ->
                b.rentalBack.setOnClickListener { vm.back() }; bookingAdapter = BookingAdapter { vm.booking(it) }; b.bookingsList.layoutManager = LinearLayoutManager(context); b.bookingsList.adapter = bookingAdapter
            }
            CatalogViewModel.Screen.BOOKING_DETAILS -> booking = FragmentBookingDetailsBinding.inflate(inflater,parent,true).also { b ->
                b.rentalBack.setOnClickListener { vm.back() }; b.cancelBooking.setOnClickListener { AlertDialog.Builder(context).setMessage("Отменить бронирование?").setNegativeButton("Назад",null).setPositiveButton("Отменить") { _,_ -> vm.cancelBooking() }.show() }
            }
            CatalogViewModel.Screen.SUCCESS -> FragmentBookingSuccessBinding.inflate(inflater,parent,true).also { b -> b.myBookings.setOnClickListener { vm.bookings() }; b.goHome.setOnClickListener { vm.home() } }
            else -> Unit
        }
    }
    fun render(ui: CatalogViewModel.Ui) {
        details?.let { b ->
            val d = ui.details
            b.rentButton.isEnabled = d != null && !ui.busy && ui.error == null
            b.favoriteButton.isEnabled = b.rentButton.isEnabled
            b.favoriteButton.setImageResource(if (ui.favorite) R.drawable.ic_lab4_heart_filled else R.drawable.ic_lab4_heart)
            b.favoriteButton.contentDescription = if (ui.favorite) "Удалить из избранного" else "Добавить в избранное"
            if (d != null) {
                b.rentalImage.setImageResource(CarAdapter.image(d.car.imageKey)); b.carName.text = "${d.car.brand} ${d.car.model}"
                b.carAddress.text = d.address; b.carDescription.text = d.description; b.carFeatures.text = "${d.car.transmission} · ${d.car.fuel}"; b.carDailyPrice.text = "${d.car.dailyPrice} ₽ / день"
            }
        }
        rent?.let { b ->
            val d = ui.details; val draft = ui.rentalDraft
            b.continueRent.isEnabled = false
            b.startDate.isEnabled = draft != null && !ui.busy; b.endDate.isEnabled = b.startDate.isEnabled
            b.driverName.isEnabled = b.startDate.isEnabled; b.driverLicense.isEnabled = b.startDate.isEnabled
            if (d != null && draft != null) {
                b.rentalImage.setImageResource(CarAdapter.image(d.car.imageKey)); b.carName.text = "${d.car.brand} ${d.car.model}"; b.carDailyPrice.text = "${d.car.dailyPrice} ₽ / день"; b.carAddress.text = d.address
                b.startDate.text = formatRentalDate(draft.start); b.endDate.text = formatRentalDate(draft.end)
                syncing = true
                if (b.driverName.text.toString() != draft.driver) b.driverName.setText(draft.driver)
                if (b.driverLicense.text.toString() != draft.license) b.driverLicense.setText(draft.license)
                syncing = false
                val price = RentalRules.price(d,draft.start,draft.end)
                b.rentCost.text = if (price == null) "Проверьте период аренды" else "Аренда (${price.days} сут.): ${price.rent} ₽"
                b.insuranceCost.text = price?.let { "Страховка: ${it.insurance} ₽" }.orEmpty()
                b.totalCost.text = price?.let { "Итого: ${it.total} ₽" }.orEmpty()
                b.depositCost.text = "Возвращаемый депозит: ${d.deposit} ₽"
                b.continueRent.isEnabled = !ui.busy && ui.error == null && RentalRules.valid(d,draft)
            }
        }
        saved?.let { b -> carAdapter?.submitList(ui.cars); b.emptySaved.isVisible = !ui.busy && ui.error == null && ui.cars.isEmpty() }
        bookings?.let { b -> bookingAdapter?.submitList(ui.bookings); b.emptyBookings.isVisible = !ui.busy && ui.error == null && ui.bookings.isEmpty() }
        booking?.let { b ->
            val item = ui.booking ?: return@let
            b.rentalTitle.text = "Бронирование №${item.id}"; b.rentalImage.setImageResource(CarAdapter.image(item.car.imageKey)); b.carName.text = "${item.car.brand} ${item.car.model}"; b.carDailyPrice.text = "${item.car.dailyPrice} ₽ / день"
            b.bookingInfo.text = "Адрес: ${item.address}\n\nНачало: ${formatRentalDate(item.start)}\nОкончание: ${formatRentalDate(item.end)}\n\nФИО водителя: ${item.driver}\nВУ: ${item.license}\nСтатус: ${bookingStatus(item)}"
            b.rentCost.text = "Аренда: ${item.rent} ₽"; b.insuranceCost.text = "Страховка: ${item.insurance} ₽"; b.totalCost.text = "Общая сумма: ${item.total} ₽"; b.depositCost.text = "Возвращаемый депозит: ${item.deposit} ₽"
            b.cancelBooking.isVisible = item.active; b.cancelBooking.isEnabled = item.active && !ui.busy
        }
    }
    private fun chooseDate(initial: Instant, callback: (Instant) -> Unit) {
        val zone = ZoneId.systemDefault(); val local = initial.atZone(zone)
        DatePickerDialog(context, { _, year, month, day ->
            val date = LocalDate.of(year,month+1,day)
            TimePickerDialog(context, { _,hour,minute -> callback(date.atTime(LocalTime.of(hour,minute)).atZone(zone).toInstant()) },local.hour,local.minute,true).show()
        },local.year,local.monthValue-1,local.dayOfMonth).apply { datePicker.minDate = System.currentTimeMillis(); show() }
    }
    fun close() { saved?.savedList?.adapter = null; bookings?.bookingsList?.adapter = null }
}
