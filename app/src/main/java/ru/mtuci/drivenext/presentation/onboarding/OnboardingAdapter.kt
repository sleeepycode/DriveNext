package ru.mtuci.drivenext.presentation.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.mtuci.drivenext.databinding.ItemOnboardingBinding

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    class PageViewHolder(
        val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PageViewHolder {

        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PageViewHolder,
        position: Int
    ) {
        val page = pages[position]

        holder.binding.apply {
            onboardingImage.setImageResource(page.image)
            onboardingTitle.setText(page.title)
            onboardingDescription.setText(page.description)
        }
    }

    override fun getItemCount(): Int {
        return pages.size
    }
}