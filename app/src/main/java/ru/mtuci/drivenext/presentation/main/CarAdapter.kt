package ru.mtuci.drivenext.presentation.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.ItemCarBinding
import ru.mtuci.drivenext.domain.model.Car

class CarAdapter(private val onDetails: (Car) -> Unit, private val onRent: (Car) -> Unit) :
    ListAdapter<Car, CarAdapter.Holder>(Diff) {
    class Holder(val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val car = getItem(position)
        with(holder.binding) {
            carModel.text = car.model
            carBrand.text = car.brand
            carPrice.text = "${car.dailyPrice} ₽ / день"
            carSpecs.text = "${car.transmission}  ·  ${car.fuel}"
            carImage.setImageResource(image(car.imageKey))
            carImage.contentDescription = "${car.brand} ${car.model}"
            carDetails.setOnClickListener { onDetails(car) }
            carRent.setOnClickListener { onRent(car) }
        }
    }
    object Diff : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Car, newItem: Car) = oldItem == newItem
    }
    companion object {
        fun image(key: String): Int = when (key) {
            "sedan_blue" -> R.drawable.ic_lab3_sedan_blue
            "sedan_white" -> R.drawable.ic_lab3_sedan_white
            "sedan_red" -> R.drawable.ic_lab3_sedan_red
            "suv_gray" -> R.drawable.ic_lab3_suv_gray
            "suv_blue" -> R.drawable.ic_lab3_suv_blue
            else -> R.drawable.ic_lab3_sedan_black
        }
    }
}
