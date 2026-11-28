package ru.mtuci.drivenext.presentation.main
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.mtuci.drivenext.databinding.ItemBookingBinding
import ru.mtuci.drivenext.domain.model.Booking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatRentalDate(value: Instant): String = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withZone(ZoneId.systemDefault()).format(value)
internal fun bookingStatus(item: Booking): String = when { item.status == "cancelled" -> "Отменено"; !item.active -> "Завершено"; else -> "Одобрено" }
class BookingAdapter(private val onClick: (Booking) -> Unit) : ListAdapter<Booking, BookingAdapter.Holder>(Diff) {
    class Holder(val binding: ItemBookingBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemBookingBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            bookingCar.text = "${item.car.brand} ${item.car.model}"
            bookingWhen.text = if (item.active) "Начало аренды: ${formatRentalDate(item.start)}" else "Окончание: ${formatRentalDate(item.end)}"
            bookingStatus.text = ru.mtuci.drivenext.presentation.main.bookingStatus(item)
            root.setOnClickListener { onClick(item) }
        }
    }
    object Diff : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(a: Booking,b: Booking) = a.id == b.id
        override fun areContentsTheSame(a: Booking,b: Booking) = a == b
    }
}
