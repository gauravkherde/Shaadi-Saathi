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
import com.gaurav.shaadisaathi.databinding.ActivityEditGuestBinding
import com.gaurav.shaadisaathi.models.Guest

class EditGuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditGuestBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var currentGuest: Guest? = null
    private val TAG = "EditGuestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupSpinners()
        setupClickListeners()
        loadGuestData()

        Log.d(TAG, "EditGuestActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Edit Guest"
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
        binding.btnUpdateGuest.setOnClickListener {
            if (validateInput()) {
                updateGuest()
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

    private fun loadGuestData() {
        val guestId = intent.getStringExtra("guestId")
        if (guestId == null) {
            Toast.makeText(this, "Guest ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("guests").document(guestId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    currentGuest = document.toObject(Guest::class.java)
                    currentGuest?.let { populateFields(it) }
                } else {
                    Toast.makeText(this, "Guest not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading guest", e)
                Toast.makeText(this, "Error loading guest: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun populateFields(guest: Guest) {
        binding.etGuestName.setText(guest.name)
        binding.etGuestPhone.setText(guest.phone)
        binding.etGuestEmail.setText(guest.email)
        binding.etRelationToHost.setText(guest.relationToHost)
        binding.etAddress.setText(guest.address)
        binding.etTableNumber.setText(if (guest.tableNumber > 0) guest.tableNumber.toString() else "")
        binding.etSpecialRequirements.setText(guest.specialRequirements)
        binding.etNotes.setText(guest.notes)

        // Set spinners
        setSpinnerSelection(binding.spinnerCategory, guest.category.replaceFirstChar { it.uppercase() })
        setSpinnerSelection(binding.spinnerMealPreference, guest.mealPreference.replaceFirstChar { it.uppercase() }.replace("nonvegetarian", "Non-Vegetarian"))
        setSpinnerSelection(binding.spinnerRsvpStatus, guest.rsvpStatus.replaceFirstChar { it.uppercase() })
        setSpinnerSelection(binding.spinnerInvitationPreference, guest.invitationPreference.replaceFirstChar { it.uppercase() })
        setSpinnerSelection(binding.spinnerLanguagePreference, guest.languagePreference.replaceFirstChar { it.uppercase() })

        // Set switches
        binding.switchPlusOne.isChecked = guest.hasPlusOne
        binding.switchVip.isChecked = guest.isVip
        binding.switchCanUploadPhotos.isChecked = guest.canUploadPhotos

        // Plus one details
        if (guest.hasPlusOne) {
            binding.layoutPlusOneDetails.visibility = View.VISIBLE
            binding.etPlusOneName.setText(guest.plusOneName)
            binding.switchPlusOneConfirmed.isChecked = guest.plusOneConfirmed
        }

        if (guest.isVip) {
            binding.tvVipNote.visibility = View.VISIBLE
        }

        Log.d(TAG, "Guest data populated: ${guest.name}")
    }

    private fun setSpinnerSelection(spinner: android.widget.Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
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

    private fun updateGuest() {
        val guest = currentGuest ?: return

        binding.btnUpdateGuest.isEnabled = false
        binding.btnUpdateGuest.text = "Updating..."
        binding.progressBar.visibility = View.VISIBLE

        val updatedGuest = guest.copy(
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
            updatedAt = System.currentTimeMillis()
        )

        firestore.collection("guests").document(guest.id)
            .set(updatedGuest)
            .addOnSuccessListener {
                Toast.makeText(this, "Guest '${updatedGuest.name}' updated successfully!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Guest updated successfully: ${guest.id}")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating guest", e)
                Toast.makeText(this, "Error updating guest: ${e.message}", Toast.LENGTH_LONG).show()
                resetUpdateButton()
            }
    }

    private fun resetUpdateButton() {
        binding.btnUpdateGuest.isEnabled = true
        binding.btnUpdateGuest.text = "Update Guest"
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
