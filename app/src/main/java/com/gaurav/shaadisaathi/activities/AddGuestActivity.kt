package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityAddGuestBinding
import com.gaurav.shaadisaathi.models.Guest
import java.util.*

class AddGuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGuestBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "AddGuestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupSpinners()
        setupClickListeners()

        Log.d(TAG, "AddGuestActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Add Guest"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("Family", "Friends", "Colleagues", "Others")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        // Meal preference spinner
        val mealPreferences = arrayOf("Vegetarian", "Non-Vegetarian", "Jain", "Vegan")
        val mealAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mealPreferences)
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealPreference.adapter = mealAdapter

        // RSVP Status spinner
        val rsvpStatuses = arrayOf("Pending", "Confirmed", "Declined")
        val rsvpAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rsvpStatuses)
        rsvpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRsvpStatus.adapter = rsvpAdapter

        // Invitation preference spinner
        val invitationPrefs = arrayOf("Digital", "Physical", "Both")
        val inviteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, invitationPrefs)
        inviteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerInvitationPreference.adapter = inviteAdapter

        // Language preference spinner
        val languages = arrayOf("English", "Hindi", "Regional")
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguagePreference.adapter = langAdapter
    }

    private fun setupClickListeners() {
        binding.btnSaveGuest.setOnClickListener {
            if (validateInput()) {
                saveGuest()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.switchPlusOne.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutPlusOneDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchVip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvVipNote.visibility = View.VISIBLE
            } else {
                binding.tvVipNote.visibility = View.GONE
            }
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.etGuestName.text.toString().trim()
        val phone = binding.etGuestPhone.text.toString().trim()
        val email = binding.etGuestEmail.text.toString().trim()

        if (name.isEmpty()) {
            binding.etGuestName.error = "Guest name is required"
            binding.etGuestName.requestFocus()
            return false
        }

        if (phone.isEmpty() && email.isEmpty()) {
            Toast.makeText(this, "Please provide either phone number or email", Toast.LENGTH_LONG).show()
            return false
        }

        if (phone.isNotEmpty() && !isValidPhoneNumber(phone)) {
            binding.etGuestPhone.error = "Please enter a valid phone number"
            binding.etGuestPhone.requestFocus()
            return false
        }

        if (email.isNotEmpty() && !isValidEmail(email)) {
            binding.etGuestEmail.error = "Please enter a valid email address"
            binding.etGuestEmail.requestFocus()
            return false
        }

        if (binding.switchPlusOne.isChecked) {
            val plusOneName = binding.etPlusOneName.text.toString().trim()
            if (plusOneName.isEmpty()) {
                binding.etPlusOneName.error = "Plus one name is required"
                binding.etPlusOneName.requestFocus()
                return false
            }
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^[+]?[0-9]{10,15}$"))
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveGuest() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save guest", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnSaveGuest.isEnabled = false
        binding.btnSaveGuest.text = "Saving..."
        binding.progressBar.visibility = View.VISIBLE

        val guestId = UUID.randomUUID().toString()
        val guest = Guest(
            id = guestId,
            hostId = currentUser.uid,
            name = binding.etGuestName.text.toString().trim(),
            email = binding.etGuestEmail.text.toString().trim(),
            phone = binding.etGuestPhone.text.toString().trim(),
            category = binding.spinnerCategory.selectedItem.toString().lowercase(),
            rsvpStatus = binding.spinnerRsvpStatus.selectedItem.toString().lowercase(),
            mealPreference = binding.spinnerMealPreference.selectedItem.toString().lowercase().replace("-", ""),
            hasPlusOne = binding.switchPlusOne.isChecked,
            plusOneName = if (binding.switchPlusOne.isChecked) binding.etPlusOneName.text.toString().trim() else "",
            plusOneConfirmed = if (binding.switchPlusOne.isChecked) binding.switchPlusOneConfirmed.isChecked else false,
            address = binding.etAddress.text.toString().trim(),
            notes = binding.etNotes.text.toString().trim(),
            specialRequirements = binding.etSpecialRequirements.text.toString().trim(),
            isVip = binding.switchVip.isChecked,
            tableNumber = binding.etTableNumber.text.toString().toIntOrNull() ?: 0,
            relationToHost = binding.etRelationToHost.text.toString().trim(),
            invitationPreference = binding.spinnerInvitationPreference.selectedItem.toString().lowercase(),
            languagePreference = binding.spinnerLanguagePreference.selectedItem.toString().lowercase(),
            canUploadPhotos = binding.switchCanUploadPhotos.isChecked,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        firestore.collection("guests").document(guestId)
            .set(guest)
            .addOnSuccessListener {
                Toast.makeText(this, "Guest '${guest.name}' added successfully!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Guest saved successfully: $guestId")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving guest", e)
                Toast.makeText(this, "Error saving guest: ${e.message}", Toast.LENGTH_LONG).show()
                resetSaveButton()
            }
    }

    private fun resetSaveButton() {
        binding.btnSaveGuest.isEnabled = true
        binding.btnSaveGuest.text = "Save Guest"
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
