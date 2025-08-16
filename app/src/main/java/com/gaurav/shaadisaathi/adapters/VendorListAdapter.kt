package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemVendorListBinding
import com.gaurav.shaadisaathi.models.Vendor
import java.text.NumberFormat
import java.util.*

class VendorListAdapter(
    private val vendors: MutableList<Vendor>,
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

    fun updateVendors(newVendors: List<Vendor>) {
        vendors.clear()
        vendors.addAll(newVendors)
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String) {
        // Implementation for filtering
        notifyDataSetChanged()
    }

    inner class VendorViewHolder(private val binding: ItemVendorListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(vendor: Vendor) {
            binding.apply {
                tvVendorName.text = vendor.getDisplayName()
                tvVendorCategory.text = vendor.getCategoryDisplayName()

                // Location display
                tvVendorLocation.text = if (vendor.location.city.isNotEmpty() && vendor.location.state.isNotEmpty()) {
                    "${vendor.location.city}, ${vendor.location.state}"
                } else if (vendor.location.city.isNotEmpty()) {
                    vendor.location.city
                } else {
                    "Location not specified"
                }

                // Price display with proper formatting
                if (vendor.pricing.basePrice > 0) {
                    val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                    tvVendorPrice.text = currency.format(vendor.pricing.basePrice)
                } else {
                    tvVendorPrice.text = "Price on request"
                }

                // Rating
                ratingBarVendor.rating = vendor.rating.toFloat()
                tvVendorRating.text = String.format("%.1f", vendor.rating)
                tvReviewCount.text = "(${vendor.totalReviews})"

                // Set vendor initial
                val initial = vendor.getDisplayName().firstOrNull()?.toString()?.uppercase() ?: "V"
                tvVendorInitial.text = initial

                // FIX: Remove isVerified reference as it doesn't exist in the model
                // Only show booked badge
                chipBooked.visibility = if (vendor.isBooked) View.VISIBLE else View.GONE

                // Favorite button
                val favoriteIcon = if (vendor.isFavorite) {
                    R.drawable.ic_favorite_filled
                } else {
                    R.drawable.ic_favorite_outline
                }
                btnFavorite.setImageResource(favoriteIcon)
                btnFavorite.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        if (vendor.isFavorite) R.color.colorPrimary else R.color.colorSecondary
                    )
                )

                // Click listeners
                root.setOnClickListener { onVendorClick(vendor) }
                btnFavorite.setOnClickListener { onFavoriteClick(vendor) }
                btnCall.setOnClickListener {
                    if (vendor.contactInfo.primaryPhone.isNotEmpty()) {
                        onCallClick(vendor)
                    }
                }
                btnBook.setOnClickListener { onBookClick(vendor) }

                // Book button state
                if (vendor.isBooked) {
                    btnBook.text = "Booked"
                    btnBook.isEnabled = false
                    btnBook.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorSecondary)
                    )
                } else {
                    btnBook.text = "Book Now"
                    btnBook.isEnabled = true
                    btnBook.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                }

                // Hide call button if no phone number
                btnCall.visibility = if (vendor.contactInfo.primaryPhone.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }
}
