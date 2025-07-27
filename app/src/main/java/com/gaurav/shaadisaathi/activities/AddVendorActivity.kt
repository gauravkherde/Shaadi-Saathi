package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityAddVendorBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.models.VendorContactInfo
import com.gaurav.shaadisaathi.models.VendorLocation
import com.gaurav.shaadisaathi.models.VendorPricing
import com.gaurav.shaadisaathi.repository.VendorRepository
import kotlinx.coroutines.launch
import java.util.*

class AddVendorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVendorBinding
    private lateinit var auth: FirebaseAuth
    private val vendorRepository = VendorRepository()
    private val TAG = "AddVendorActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVendorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupSpinners()
        setupClickListeners()

        Log.d(TAG, "AddVendorActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Add Vendor"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupSpinners() {
        // Vendor Category
        val categories = arrayOf("Photographer", "Caterer", "Decorator", "DJ", "Makeup Artist", "Florist", "Transportation", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVendorCategory.adapter = categoryAdapter

        // Rating (optional)
        val ratings = arrayOf("Not Rated", "1 Star", "2 Stars", "3 Stars", "4 Stars", "5 Stars")
        val ratingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ratings)
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRating.adapter = ratingAdapter
    }

    private fun setupClickListeners() {
        binding.btnSaveVendor.setOnClickListener {
            if (validateInput()) {
                saveVendor()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.switchIsVerified.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvVerifiedNote.visibility = View.VISIBLE
            } else {
                binding.tvVerifiedNote.visibility = View.GONE
            }
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.etVendorName.text.toString().trim()
        val businessName = binding.etBusinessName.text.toString().trim()
        val phone = binding.etVendorPhone.text.toString().trim()
        val email = binding.etVendorEmail.text.toString().trim()

        if (name.isEmpty() && businessName.isEmpty()) {
            Toast.makeText(this, "Please provide either vendor name or business name", Toast.LENGTH_LONG).show()
            return false
        }

        if (phone.isEmpty() && email.isEmpty()) {
            Toast.makeText(this, "Please provide either phone number or email", Toast.LENGTH_LONG).show()
            return false
        }

        if (phone.isNotEmpty() && !isValidPhoneNumber(phone)) {
            binding.etVendorPhone.error = "Please enter a valid phone number"
            binding.etVendorPhone.requestFocus()
            return false
        }

        if (email.isNotEmpty() && !isValidEmail(email)) {
            binding.etVendorEmail.error = "Please enter a valid email address"
            binding.etVendorEmail.requestFocus()
            return false
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^[+]?[0-9]{10,15}$"))
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveVendor() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save vendor", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnSaveVendor.isEnabled = false
        binding.btnSaveVendor.text = "Saving..."
        binding.progressBar.visibility = View.VISIBLE

        val vendorId = UUID.randomUUID().toString()

        val contactInfo = VendorContactInfo(
            primaryPhone = binding.etVendorPhone.text.toString().trim(),
            secondaryPhone = binding.etSecondaryPhone.text.toString().trim(),
            email = binding.etVendorEmail.text.toString().trim(),
            website = binding.etWebsite.text.toString().trim(),
            socialMedia = binding.etSocialMedia.text.toString().trim()
        )

        val location = VendorLocation(
            address = binding.etVendorAddress.text.toString().trim(),
            city = binding.etVendorCity.text.toString().trim(),
            state = binding.etVendorState.text.toString().trim(),
            pincode = binding.etVendorPincode.text.toString().trim()
        )

        val basePrice = binding.etBasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val pricing = VendorPricing(
            basePrice = basePrice,
            currency = "INR",
            priceRange = if (basePrice > 0) {
                when {
                    basePrice < 10000 -> "Budget"
                    basePrice < 50000 -> "Mid-range"
                    else -> "Premium"
                }
            } else "Not specified",
            negotiable = binding.switchNegotiable.isChecked
        )

        val rating = when (binding.spinnerRating.selectedItemPosition) {
            0 -> 0.0 // Not Rated
            else -> binding.spinnerRating.selectedItemPosition.toDouble()
        }

        val vendor = Vendor(
            id = vendorId,
            hostId = currentUser.uid,
            name = binding.etVendorName.text.toString().trim(),
            businessName = binding.etBusinessName.text.toString().trim(),
            category = binding.spinnerVendorCategory.selectedItem.toString().lowercase().replace(" ", ""),
            description = binding.etVendorDescription.text.toString().trim(),
            contactInfo = contactInfo,
            location = location,
            pricing = pricing,
            rating = rating,
            totalReviews = if (rating > 0) 1 else 0,
            services = binding.etServices.text.toString().trim(),
            specialization = binding.etSpecialization.text.toString().trim(),
            experience = binding.etExperience.text.toString().toIntOrNull() ?: 0,
            portfolio = binding.etPortfolio.text.toString().trim(),
            notes = binding.etVendorNotes.text.toString().trim(),
            isVerified = binding.switchIsVerified.isChecked,
            isFavorite = binding.switchFavorite.isChecked,
            addedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                val result = vendorRepository.addVendor(vendor)
                if (result.isSuccess) {
                    val displayName = vendor.getDisplayName()
                    Toast.makeText(this@AddVendorActivity, "Vendor '$displayName' added successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Vendor saved successfully: $vendorId")
                    finish()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error saving vendor", error)
                    Toast.makeText(this@AddVendorActivity, "Error saving vendor: ${error?.message}", Toast.LENGTH_LONG).show()
                    resetSaveButton()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving vendor", e)
                Toast.makeText(this@AddVendorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetSaveButton()
            }
        }
    }

    private fun resetSaveButton() {
        binding.btnSaveVendor.isEnabled = true
        binding.btnSaveVendor.text = "Save Vendor"
        binding.progressBar.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
