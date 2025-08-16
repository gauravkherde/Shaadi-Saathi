package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaurav.shaadisaathi.databinding.ActivityEditVendorBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.models.VendorContactInfo
import com.gaurav.shaadisaathi.models.VendorLocation
import com.gaurav.shaadisaathi.models.VendorPricing
import com.gaurav.shaadisaathi.repository.VendorRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditVendorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditVendorBinding
    private val vendorRepository = VendorRepository()
    private val auth = FirebaseAuth.getInstance()

    private var vendor: Vendor? = null
    private var vendorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVendorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vendorId = intent.getStringExtra("vendorId") ?: ""
        if (vendorId.isEmpty()) {
            Toast.makeText(this, "Invalid vendor ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupClickListeners()
        loadVendorData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Edit Vendor"
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveVendor()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadVendorData() {
        lifecycleScope.launch {
            try {
                val result = vendorRepository.getVendorById(vendorId)
                if (result.isSuccess) {
                    vendor = result.getOrNull()
                    vendor?.let { populateFields(it) }
                } else {
                    Toast.makeText(this@EditVendorActivity, "Error loading vendor", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditVendorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateFields(vendor: Vendor) {
        binding.apply {
            etVendorName.setText(vendor.name)
            etDescription.setText(vendor.description)
            etServices.setText(vendor.services)
            etSpecialization.setText(vendor.specialization)
            etExperience.setText(vendor.experience)

            // Contact info
            etPrimaryPhone.setText(vendor.contactInfo.primaryPhone)
            etSecondaryPhone.setText(vendor.contactInfo.secondaryPhone)
            etEmail.setText(vendor.contactInfo.email)
            etWebsite.setText(vendor.contactInfo.website)

            // Location
            etAddress.setText(vendor.location.address)
            etCity.setText(vendor.location.city)
            etState.setText(vendor.location.state)
            etPincode.setText(vendor.location.pincode)

            // Pricing
            etBasePrice.setText(vendor.pricing.basePrice.toString())
            etPriceRange.setText(vendor.pricing.priceRange)
            etPerHourRate.setText(vendor.pricing.perHourRate.toString())

            etNotes.setText(vendor.notes)
        }
    }

    private fun saveVendor() {
        if (!validateFields()) return

        lifecycleScope.launch {
            try {
                val updatedVendor = createUpdatedVendor()
                val result = vendorRepository.updateVendor(updatedVendor)

                if (result.isSuccess) {
                    Toast.makeText(this@EditVendorActivity, "Vendor updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditVendorActivity, "Error updating vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditVendorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateFields(): Boolean {
        binding.apply {
            if (etVendorName.text.toString().trim().isEmpty()) {
                etVendorName.error = "Vendor name is required"
                return false
            }
        }
        return true
    }

    private fun createUpdatedVendor(): Vendor {
        val currentVendor = vendor ?: throw IllegalStateException("Vendor not loaded")

        return currentVendor.copy(
            name = binding.etVendorName.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            services = binding.etServices.text.toString().trim(),
            specialization = binding.etSpecialization.text.toString().trim(),
            experience = binding.etExperience.text.toString().trim(),
            contactInfo = VendorContactInfo(
                primaryPhone = binding.etPrimaryPhone.text.toString().trim(),
                secondaryPhone = binding.etSecondaryPhone.text.toString().trim(),
                email = binding.etEmail.text.toString().trim(),
                website = binding.etWebsite.text.toString().trim()
            ),
            location = VendorLocation(
                address = binding.etAddress.text.toString().trim(),
                city = binding.etCity.text.toString().trim(),
                state = binding.etState.text.toString().trim(),
                pincode = binding.etPincode.text.toString().trim()
            ),
            pricing = VendorPricing(
                basePrice = binding.etBasePrice.text.toString().toDoubleOrNull() ?: 0.0,
                priceRange = binding.etPriceRange.text.toString().trim(),
                perHourRate = binding.etPerHourRate.text.toString().toDoubleOrNull() ?: 0.0
            ),
            notes = binding.etNotes.text.toString().trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
