package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.VendorDetailAdapter
import com.gaurav.shaadisaathi.adapters.VendorDetailItem
import com.gaurav.shaadisaathi.databinding.ActivityVendorDetailBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.repository.VendorRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class VendorDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVendorDetailBinding
    private lateinit var detailAdapter: VendorDetailAdapter
    private val detailItems = mutableListOf<VendorDetailItem>()
    private val vendorRepository = VendorRepository()
    private val auth = FirebaseAuth.getInstance()

    private var vendor: Vendor? = null
    private var vendorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendorDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vendorId = intent.getStringExtra("vendorId") ?: ""
        if (vendorId.isEmpty()) {
            Toast.makeText(this, "Invalid vendor ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadVendorDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Vendor Details"
        }
    }

    private fun setupRecyclerView() {
        detailAdapter = VendorDetailAdapter(detailItems)

        binding.recyclerViewDetails.apply {
            layoutManager = LinearLayoutManager(this@VendorDetailActivity)
            adapter = detailAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCall.setOnClickListener {
            vendor?.let { callVendor(it) }
        }

        binding.btnEmail.setOnClickListener {
            vendor?.let { emailVendor(it) }
        }

        binding.btnWebsite.setOnClickListener {
            vendor?.let { openWebsite(it) }
        }

        binding.btnFavorite.setOnClickListener {
            vendor?.let { toggleFavorite(it) }
        }

        binding.btnBook.setOnClickListener {
            vendor?.let { bookVendor(it) }
        }

        binding.btnShare.setOnClickListener {
            vendor?.let { shareVendor(it) }
        }
    }

    private fun loadVendorDetails() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = vendorRepository.getVendorById(vendorId)
                if (result.isSuccess) {
                    vendor = result.getOrNull()
                    vendor?.let { displayVendorDetails(it) }
                } else {
                    Toast.makeText(this@VendorDetailActivity, "Error loading vendor details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VendorDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun displayVendorDetails(vendor: Vendor) {
        binding.apply {
            // Header information
            tvVendorName.text = vendor.getDisplayName()
            tvVendorCategory.text = vendor.getCategoryDisplayName()
            tvVendorRating.text = String.format("%.1f", vendor.rating)
            ratingBar.rating = vendor.rating.toFloat()
            tvReviewCount.text = "(${vendor.totalReviews} reviews)"

            // Description
            tvVendorDescription.text = if (vendor.description.isNotEmpty()) {
                vendor.description
            } else {
                "No description available"
            }

            // Update favorite button
            btnFavorite.setImageResource(
                if (vendor.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outline
            )

            // Update book button
            btnBook.text = if (vendor.isBooked) "Booked" else "Book Now"
            btnBook.isEnabled = !vendor.isBooked

            // Show/hide action buttons based on available contact info
            btnCall.visibility = if (vendor.contactInfo.primaryPhone.isNotEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
            btnEmail.visibility = if (vendor.contactInfo.email.isNotEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
            btnWebsite.visibility = if (vendor.contactInfo.website.isNotEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
        }

        // Setup detail items
        setupDetailItems(vendor)
        supportActionBar?.title = vendor.getDisplayName()
    }

    private fun setupDetailItems(vendor: Vendor) {
        detailItems.clear()

        // Contact Information
        if (vendor.contactInfo.primaryPhone.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_phone,
                    title = "Phone",
                    value = vendor.contactInfo.primaryPhone
                )
            )
        }

        if (vendor.contactInfo.secondaryPhone.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_phone,
                    title = "Secondary Phone",
                    value = vendor.contactInfo.secondaryPhone
                )
            )
        }

        if (vendor.contactInfo.email.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_email,
                    title = "Email",
                    value = vendor.contactInfo.email
                )
            )
        }

        if (vendor.contactInfo.website.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_web,
                    title = "Website",
                    value = vendor.contactInfo.website
                )
            )
        }

        // Location Information
        if (vendor.location.address.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_location,
                    title = "Address",
                    value = vendor.location.getFullAddress()
                )
            )
        }

        // Pricing Information
        if (vendor.pricing.basePrice > 0) {
            val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_placeholder,
                    title = "Base Price",
                    value = currency.format(vendor.pricing.basePrice)
                )
            )
        }

        if (vendor.pricing.priceRange.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_placeholder,
                    title = "Price Range",
                    value = vendor.pricing.priceRange
                )
            )
        }

        if (vendor.pricing.perHourRate > 0) {
            val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_time,
                    title = "Per Hour Rate",
                    value = "${currency.format(vendor.pricing.perHourRate)}/hour"
                )
            )
        }

        // Additional Information
        if (vendor.services.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_vendor_placeholder,
                    title = "Services",
                    value = vendor.services
                )
            )
        }

        if (vendor.specialization.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_vip_star,
                    title = "Specialization",
                    value = vendor.specialization
                )
            )
        }

        if (vendor.experience.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_placeholder,
                    title = "Experience",
                    value = vendor.experience
                )
            )
        }

        if (vendor.notes.isNotEmpty()) {
            detailItems.add(
                VendorDetailItem(
                    icon = R.drawable.ic_placeholder,
                    title = "Notes",
                    value = vendor.notes
                )
            )
        }

        detailAdapter.notifyDataSetChanged()
    }

    private fun callVendor(vendor: Vendor) {
        val phoneNumber = vendor.contactInfo.primaryPhone
        if (phoneNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun emailVendor(vendor: Vendor) {
        val email = vendor.contactInfo.email
        if (email.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Wedding Service Inquiry")
                putExtra(Intent.EXTRA_TEXT, "Hi ${vendor.name},\n\nI'm interested in your services for my wedding. Please let me know your availability.\n\nThanks!")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWebsite(vendor: Vendor) {
        val website = vendor.contactInfo.website
        if (website.isNotEmpty()) {
            var url = website
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open website", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFavorite(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                val updatedVendor = vendor.copy(
                    isFavorite = !vendor.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    this@VendorDetailActivity.vendor = updatedVendor
                    binding.btnFavorite.setImageResource(
                        if (updatedVendor.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_outline
                    )

                    val message = if (updatedVendor.isFavorite) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(this@VendorDetailActivity, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VendorDetailActivity, "Error updating favorite status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VendorDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bookVendor(vendor: Vendor) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Book ${vendor.getDisplayName()}")
            .setMessage("Are you sure you want to book this vendor for your wedding?")
            .setPositiveButton("Book") { _, _ ->
                performBooking(vendor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBooking(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE

                val updatedVendor = vendor.copy(
                    isBooked = true,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    this@VendorDetailActivity.vendor = updatedVendor
                    binding.btnBook.text = "Booked"
                    binding.btnBook.isEnabled = false

                    Toast.makeText(this@VendorDetailActivity, "Vendor booked successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@VendorDetailActivity, "Error booking vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VendorDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun shareVendor(vendor: Vendor) {
        val shareText = buildString {
            append("Check out this vendor for weddings:\n\n")
            append("${vendor.getDisplayName()}\n")
            append("Category: ${vendor.getCategoryDisplayName()}\n")
            append("Rating: ${vendor.rating}/5.0 (${vendor.totalReviews} reviews)\n")

            if (vendor.contactInfo.primaryPhone.isNotEmpty()) {
                append("Phone: ${vendor.contactInfo.primaryPhone}\n")
            }

            if (vendor.contactInfo.email.isNotEmpty()) {
                append("Email: ${vendor.contactInfo.email}\n")
            }

            if (vendor.contactInfo.website.isNotEmpty()) {
                append("Website: ${vendor.contactInfo.website}\n")
            }

            append("\nShared via ShaadiSaathi Wedding Planner")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Wedding Vendor Recommendation")
        }

        startActivity(Intent.createChooser(intent, "Share Vendor"))
    }

    private fun editVendor() {
        val intent = Intent(this, EditVendorActivity::class.java)
        intent.putExtra("vendorId", vendorId)
        startActivity(intent)
    }

    private fun deleteVendor() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Vendor")
            .setMessage("Are you sure you want to delete this vendor? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDelete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE

                val result = vendorRepository.deleteVendor(vendorId)
                if (result.isSuccess) {
                    Toast.makeText(this@VendorDetailActivity, "Vendor deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@VendorDetailActivity, "Error deleting vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VendorDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_vendor_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                editVendor()
                true
            }
            R.id.action_delete -> {
                deleteVendor()
                true
            }
            R.id.action_share -> {
                vendor?.let { shareVendor(it) }
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadVendorDetails() // Refresh data when returning from edit
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
