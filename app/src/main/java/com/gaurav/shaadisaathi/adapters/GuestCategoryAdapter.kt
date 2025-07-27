package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemCategorySelectorBinding
import com.gaurav.shaadisaathi.models.GuestCategory

class GuestCategoryAdapter(
    private var categories: List<GuestCategory>,
    private val onCategoryClick: (GuestCategory) -> Unit
) : RecyclerView.Adapter<GuestCategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(private val binding: ItemCategorySelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: GuestCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                tvCategoryDescription.text = category.description
                tvCategoryCount.text = category.guestCount.toString()

                // Set category icon
                val iconResource = when (category.icon) {
                    "ic_family" -> R.drawable.ic_family
                    "ic_friends" -> R.drawable.ic_friends
                    "ic_colleagues" -> R.drawable.ic_colleagues
                    else -> R.drawable.ic_guest
                }
                ivCategoryIcon.setImageResource(iconResource)

                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    fun updateCategories(newCategories: List<GuestCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
