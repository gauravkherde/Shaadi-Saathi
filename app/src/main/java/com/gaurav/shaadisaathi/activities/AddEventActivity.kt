package com.gaurav.shaadisaathi.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityAddEventBinding
import com.gaurav.shaadisaathi.models.Event
import java.util.*
import android.content.Context
import android.net.ConnectivityManager


class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var editingEventId: String? = null
    private val TAG = "AddEventActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        editingEventId = intent.getStringExtra("eventId")

        setupEventTypeSpinner()
        setupDateTimePickers()

        if (editingEventId != null) {
            binding.tvTitle.text = "Edit Event"
            binding.btnSaveEvent.text = "Update Event"
            loadEventData(editingEventId!!)
        }

        binding.btnSaveEvent.setOnClickListener {
            saveEvent()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupEventTypeSpinner() {
        val eventTypes = arrayOf("Mehendi", "Sangeet", "Wedding", "Reception", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter
    }

    private fun setupDateTimePickers() {
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.etTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                binding.etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                binding.etTime.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun loadEventData(eventId: String) {
        binding.btnSaveEvent.isEnabled = false
        binding.btnSaveEvent.text = "Loading..."

        firestore.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                binding.btnSaveEvent.isEnabled = true
                binding.btnSaveEvent.text = "Update Event"

                val event = doc.toObject(Event::class.java)
                event?.let {
                    binding.etEventName.setText(it.name)
                    binding.etDescription.setText(it.description)
                    binding.etDate.setText(it.date)
                    binding.etTime.setText(it.time)
                    binding.etVenue.setText(it.venue)
                    binding.etAddress.setText(it.address)

                    // Set event type spinner
                    val eventTypes = arrayOf("Mehendi", "Sangeet", "Wedding", "Reception", "Other")
                    val position = eventTypes.indexOfFirst { type ->
                        type.equals(it.type, ignoreCase = true)
                    }
                    if (position >= 0) {
                        binding.spinnerEventType.setSelection(position)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading event data", e)
                binding.btnSaveEvent.isEnabled = true
                binding.btnSaveEvent.text = "Update Event"
                Toast.makeText(this, "Error loading event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveEvent() {
        // Get form data
        val name = binding.etEventName.text.toString().trim()
        val type = binding.spinnerEventType.selectedItem.toString().lowercase()
        val description = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val venue = binding.etVenue.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.etEventName.error = "Event name is required"
            binding.etEventName.requestFocus()
            return
        }

        if (date.isEmpty()) {
            binding.etDate.error = "Date is required"
            binding.etDate.requestFocus()
            return
        }

        if (time.isEmpty()) {
            binding.etTime.error = "Time is required"
            binding.etTime.requestFocus()
            return
        }

        if (venue.isEmpty()) {
            binding.etVenue.error = "Venue is required"
            binding.etVenue.requestFocus()
            return
        }

        // Check user authentication
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show loading
        binding.btnSaveEvent.isEnabled = false
        binding.btnSaveEvent.text = if (editingEventId != null) "Updating..." else "Saving..."

        // Create event ID
        val eventId = editingEventId ?: firestore.collection("events").document().id

        // Create event object
        val event = Event(
            id = eventId,
            name = name,
            type = type,
            description = description,
            date = date,
            time = time,
            venue = venue,
            address = address,
            hostId = currentUser.uid,
            createdAt = if (editingEventId != null) 0L else System.currentTimeMillis()
        )

        Log.d(TAG, "Attempting to save event: $eventId")
        Log.d(TAG, "Event data: $event")

        // Save to Firestore
        firestore.collection("events").document(eventId).set(event)
            .addOnSuccessListener {
                Log.d(TAG, "Event saved successfully with ID: $eventId")

                // Reset button state
                binding.btnSaveEvent.isEnabled = true
                binding.btnSaveEvent.text = if (editingEventId != null) "Update Event" else "Save Event"

                // Show success message
                val message = if (editingEventId != null) "Event updated successfully" else "Event added successfully"
                Toast.makeText(this@AddEventActivity, message, Toast.LENGTH_SHORT).show()

                // Finish activity with result
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving event", e)

                // Reset button state
                binding.btnSaveEvent.isEnabled = true
                binding.btnSaveEvent.text = if (editingEventId != null) "Update Event" else "Save Event"

                // Show error message
                Toast.makeText(this@AddEventActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Add this function to your AddEventActivity.kt class
    private fun checkNetworkAndFirestore() {
        Log.d(TAG, "Checking Firestore network state...")

        firestore.enableNetwork()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Firestore network enabled successfully")
                Toast.makeText(this, "Network connected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Firestore network error: ${e.message}", e)
                Toast.makeText(this, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}
