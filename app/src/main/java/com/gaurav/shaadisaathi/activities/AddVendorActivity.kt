package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityAddVendorBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.models.VendorContactInfo
import com.gaurav.shaadisaathi.models.VendorLocation
import com.gaurav.shaadisaathi.models.VendorPricing
import com.gaurav.shaadisaathi.repository.VendorRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AddVendorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVendorBinding
    private val vendorRepository = VendorRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVendorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinners()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Vendor"
        }
    }

    private fun setupSpinners() {
        // Vendor Category Spinner
        val categories = arrayOf(
            "Photographer", "Caterer", "Decorator", "DJ/Music",
            "Makeup Artist", "Florist", "Venue", "Transport",
            "Videographer", "Mehendi Artist", "Pandit/Priest",
            "Security", "Sound System", "Lighting", "Others"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        binding.btnSaveVendor.setOnClickListener {
            if (validateInputs()) {
                saveVendor()
            }
        }

        binding.btnCancel.setOnClickListener {
            showCancelConfirmation()
        }

        binding.btnClearForm.setOnClickListener {
            clearForm()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        with(binding) {
            // Vendor name validation
            if (etVendorName.text.toString().trim().isEmpty()) {
                etVendorName.error = "Vendor name is required"
                etVendorName.requestFocus()
                isValid = false
            }

            // Primary phone validation
            if (etPrimaryPhone.text.toString().trim().isEmpty()) {
                etPrimaryPhone.error = "Phone number is required"
                if (isValid) etPrimaryPhone.requestFocus()
                isValid = false
            } else if (etPrimaryPhone.text.toString().trim().length < 10) {
                etPrimaryPhone.error = "Enter a valid phone number"
                if (isValid) etPrimaryPhone.requestFocus()
                isValid = false
            }

            // Email validation (if provided)
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email address"
                if (isValid) etEmail.requestFocus()
                isValid = false
            }

            // City validation
            if (etCity.text.toString().trim().isEmpty()) {
                etCity.error = "City is required"
                if (isValid) etCity.requestFocus()
                isValid = false
            }

            // State validation
            if (etState.text.toString().trim().isEmpty()) {
                etState.error = "State is required"
                if (isValid) etState.requestFocus()
                isValid = false
            }

            // Base price validation
            val basePriceText = etBasePrice.text.toString().trim()
            if (basePriceText.isEmpty()) {
                etBasePrice.error = "Base price is required"
                if (isValid) etBasePrice.requestFocus()
                isValid = false
            } else {
                val basePrice = basePriceText.toDoubleOrNull()
                if (basePrice == null || basePrice <= 0) {
                    etBasePrice.error = "Enter a valid price"
                    if (isValid) etBasePrice.requestFocus()
                    isValid = false
                }
            }

            // Per hour rate validation (if provided)
            val perHourRateText = etPerHourRate.text.toString().trim()
            if (perHourRateText.isNotEmpty()) {
                val perHourRate = perHourRateText.toDoubleOrNull()
                if (perHourRate == null || perHourRate <= 0) {
                    etPerHourRate.error = "Enter a valid rate"
                    if (isValid) etPerHourRate.requestFocus()
                    isValid = false
                }
            }
        }
        return isValid
    }

    private fun saveVendor() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSaveVendor.isEnabled = false

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@AddVendorActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val contactInfo = VendorContactInfo(
                    primaryPhone = binding.etPrimaryPhone.text.toString().trim(),
                    secondaryPhone = binding.etSecondaryPhone.text.toString().trim(),
                    email = binding.etEmail.text.toString().trim(),
                    website = binding.etWebsite.text.toString().trim()
                )

                val location = VendorLocation(
                    address = binding.etAddress.text.toString().trim(),
                    city = binding.etCity.text.toString().trim(),
                    state = binding.etState.text.toString().trim(),
                    pincode = binding.etPincode.text.toString().trim(),
                    latitude = 0.0,
                    longitude = 0.0
                )

                val pricing = VendorPricing(
                    basePrice = binding.etBasePrice.text.toString().toDoubleOrNull() ?: 0.0,
                    priceRange = binding.etPriceRange.text.toString().trim(),
                    perHourRate = binding.etPerHourRate.text.toString().toDoubleOrNull() ?: 0.0,
                    currency = "INR"
                )

                val vendor = Vendor(
                    id = System.currentTimeMillis().toString(),
                    name = binding.etVendorName.text.toString().trim(),
                    category = binding.spinnerCategory.selectedItem.toString(),
                    description = binding.etDescription.text.toString().trim(),
                    contactInfo = contactInfo,
                    location = location,
                    pricing = pricing,
                    services = binding.etServices.text.toString().trim(),
                    specialization = binding.etSpecialization.text.toString().trim(),
                    experience = binding.etExperience.text.toString().trim(),
                    rating = 0.0,
                    totalReviews = 0,
                    imageUrls = emptyList(),
                    isFavorite = false,
                    isBooked = false,
                    notes = binding.etNotes.text.toString().trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.addVendor(vendor)
                if (result.isSuccess) {
                    Toast.makeText(this@AddVendorActivity, "Vendor added successfully! 🎉", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddVendorActivity, "Error adding vendor. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddVendorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSaveVendor.isEnabled = true
            }
        }
    }

    private fun clearForm() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Form")
            .setMessage("Are you sure you want to clear all entered data?")
            .setPositiveButton("Clear") { _, _ ->
                performClearForm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performClearForm() {
        with(binding) {
            etVendorName.text?.clear()
            etDescription.text?.clear()
            etServices.text?.clear()
            etSpecialization.text?.clear()
            etExperience.text?.clear()
            etPrimaryPhone.text?.clear()
            etSecondaryPhone.text?.clear()
            etEmail.text?.clear()
            etWebsite.text?.clear()
            etAddress.text?.clear()
            etCity.text?.clear()
            etState.text?.clear()
            etPincode.text?.clear()
            etBasePrice.text?.clear()
            etPriceRange.text?.clear()
            etPerHourRate.text?.clear()
            etNotes.text?.clear()
            spinnerCategory.setSelection(0)

            // Clear all errors
            etVendorName.error = null
            etPrimaryPhone.error = null
            etEmail.error = null
            etCity.error = null
            etState.error = null
            etBasePrice.error = null
            etPerHourRate.error = null
        }

        Toast.makeText(this, "Form cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showCancelConfirmation() {
        // Check if any field has data
        val hasData = with(binding) {
            etVendorName.text.toString().trim().isNotEmpty() ||
                    etDescription.text.toString().trim().isNotEmpty() ||
                    etServices.text.toString().trim().isNotEmpty() ||
                    etPrimaryPhone.text.toString().trim().isNotEmpty() ||
                    etEmail.text.toString().trim().isNotEmpty() ||
                    etCity.text.toString().trim().isNotEmpty() ||
                    etBasePrice.text.toString().trim().isNotEmpty()
        }

        if (hasData) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Discard Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Discard") { _, _ ->
                    finish()
                }
                .setNegativeButton("Continue Editing", null)
                .show()
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelConfirmation()
        return true
    }

    override fun onBackPressed() {
        showCancelConfirmation()
    }
}
