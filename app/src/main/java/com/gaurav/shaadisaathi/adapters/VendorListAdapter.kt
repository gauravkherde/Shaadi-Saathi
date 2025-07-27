package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemVendorListBinding
import com.gaurav.shaadisaathi.models.Vendor

class VendorListAdapter(
    private val vendors: List<Vendor>,
    private val onVendorClick: (Vendor) -> Unit,
    private val onFavoriteClick: (Vendor) -> Unit,
    private val onBookClick: (Vendor) -> Unit,
    private val onCallClick: (Vendor) -> Unit
) : RecyclerView.Adapter<VendorListAdapter.VendorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val binding = ItemVendorListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VendorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        holder.bind(vendors[position])
    }

    override fun getItemCount(): Int = vendors.size

    inner class VendorViewHolder(private val binding: ItemVendorListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(vendor: Vendor) {
            binding.apply {
                tvVendorName.text = vendor.getDisplayName()
                tvVendorCategory.text = vendor.getCategoryDisplayName()
                tvVendorLocation.text = "${vendor.location.city}, ${vendor.location.state}"
                tvVendorPrice.text = "₹${vendor.pricing.basePrice}"

                ratingBarVendor.rating = vendor.rating.toFloat()
                tvVendorRating.text = vendor.rating.toString()

                // Set vendor initial
                val initial = vendor.getDisplayName().firstOrNull()?.toString()?.uppercase() ?: "V"
                tvVendorInitial.text = initial

                // Show/hide badges
                chipVerified.visibility = if (vendor.isVerified) View.VISIBLE else View.GONE
                chipBooked.visibility = if (vendor.isBooked) View.VISIBLE else View.GONE

                // Favorite button
                val favoriteIcon = if (vendor.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                btnFavorite.setImageResource(favoriteIcon)
                btnFavorite.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        if (vendor.isFavorite) R.color.colorAccent else R.color.colorSecondary
                    )
                )

                // Click listeners
                root.setOnClickListener { onVendorClick(vendor) }
                btnFavorite.setOnClickListener { onFavoriteClick(vendor) }
                btnCall.setOnClickListener { onCallClick(vendor) }
                btnBook.setOnClickListener { onBookClick(vendor) }

                // Book button state
                if (vendor.isBooked) {
                    btnBook.text = "Booked"
                    btnBook.isEnabled = false
                } else {
                    btnBook.text = "Book"
                    btnBook.isEnabled = true
                }
            }
        }
    }
}
