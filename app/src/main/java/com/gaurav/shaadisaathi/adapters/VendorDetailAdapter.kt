package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemVendorDetailBinding

data class VendorDetailItem(
    val title: String,
    val value: String,
    val icon: Int
)

class VendorDetailAdapter(
    private val details: List<VendorDetailItem>
) : RecyclerView.Adapter<VendorDetailAdapter.DetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemVendorDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(details[position])
    }

    override fun getItemCount(): Int = details.size

    inner class DetailViewHolder(private val binding: ItemVendorDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(detail: VendorDetailItem) {
            binding.apply {
                ivDetailIcon.setImageResource(detail.icon)
                tvDetailTitle.text = detail.title
                tvDetailValue.text = detail.value
            }
        }
    }
}
