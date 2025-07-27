package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityAddGuestBinding
import com.gaurav.shaadisaathi.models.Guest

class AddGuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGuestBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var editingGuestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        editingGuestId = intent.getStringExtra("guestId")

        setupCategorySpinner()

        if (editingGuestId != null) {
            binding.tvTitle.text = "Edit Guest"
            binding.btnSaveGuest.text = "Update Guest"
            loadGuestData(editingGuestId!!)
        }

        binding.btnSaveGuest.setOnClickListener {
            saveGuest()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Family", "Friends", "Colleagues", "VIPs", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun loadGuestData(guestId: String) {
        firestore.collection("guests").document(guestId).get()
            .addOnSuccessListener { doc ->
                val guest = doc.toObject(Guest::class.java)
                guest?.let {
                    binding.etName.setText(it.name)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phone)
                    binding.etDietaryRestrictions.setText(it.dietaryRestrictions)
                    binding.switchPlusOne.isChecked = it.plusOne

                    // Set category spinner
                    val categories = arrayOf("Family", "Friends", "Colleagues", "VIPs", "Other")
                    val position = categories.indexOf(it.category)
                    if (position >= 0) {
                        binding.spinnerCategory.setSelection(position)
                    }
                }
            }
    }

    private fun saveGuest() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val dietaryRestrictions = binding.etDietaryRestrictions.text.toString().trim()
        val plusOne = binding.switchPlusOne.isChecked

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email"
            return
        }

        val currentUser = auth.currentUser ?: return

        binding.btnSaveGuest.isEnabled = false
        binding.btnSaveGuest.text = if (editingGuestId != null) "Updating..." else "Saving..."

        val guestId = editingGuestId ?: firestore.collection("guests").document().id

        val guest = Guest(
            id = guestId,
            name = name,
            email = email,
            phone = phone,
            category = category,
            dietaryRestrictions = dietaryRestrictions,
            plusOne = plusOne,
            hostId = currentUser.uid,
            createdAt = if (editingGuestId != null) 0L else System.currentTimeMillis()
        )

        firestore.collection("guests").document(guestId).set(guest)
            .addOnSuccessListener {
                Toast.makeText(this, "Guest ${if (editingGuestId != null) "updated" else "added"} successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSaveGuest.isEnabled = true
                binding.btnSaveGuest.text = if (editingGuestId != null) "Update Guest" else "Save Guest"
            }
    }
}
