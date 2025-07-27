package com.gaurav.shaadisaathi.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.gaurav.shaadisaathi.databinding.ActivityAddEventBinding
import com.gaurav.shaadisaathi.models.Event
import com.gaurav.shaadisaathi.models.EventVenue
import com.gaurav.shaadisaathi.repository.EventRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding
    private lateinit var auth: FirebaseAuth
    private val eventRepository = EventRepository()
    private var selectedDate: Long = 0L
    private val TAG = "AddEventActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupSpinners()
        setupClickListeners()

        Log.d(TAG, "AddEventActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Add Event"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupSpinners() {
        val eventTypes = arrayOf("Wedding", "Mehendi", "Sangam", "Reception", "Engagement", "Haldi", "Other")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = typeAdapter
    }

    private fun setupClickListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectStartTime.setOnClickListener {
            showTimePicker(true)
        }

        binding.btnSelectEndTime.setOnClickListener {
            showTimePicker(false)
        }

        binding.btnSaveEvent.setOnClickListener {
            if (validateInput()) {
                saveEvent()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 12, 0, 0)
            selectedDate = calendar.timeInMillis

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvSelectedDate.text = dateFormat.format(Date(selectedDate))
            binding.tvSelectedDate.setTextColor(getColor(R.color.colorPrimary))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()

        TimePickerDialog(this, { _, hourOfDay, minute ->
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeCalendar = Calendar.getInstance()
            timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            timeCalendar.set(Calendar.MINUTE, minute)

            val timeString = timeFormat.format(timeCalendar.time)

            if (isStartTime) {
                binding.tvSelectedStartTime.text = timeString
                binding.tvSelectedStartTime.setTextColor(getColor(R.color.colorPrimary))
            } else {
                binding.tvSelectedEndTime.text = timeString
                binding.tvSelectedEndTime.setTextColor(getColor(R.color.colorPrimary))
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    private fun validateInput(): Boolean {
        val eventName = binding.etEventName.text.toString().trim()
        val venueName = binding.etVenueName.text.toString().trim()

        if (eventName.isEmpty()) {
            binding.etEventName.error = "Event name is required"
            binding.etEventName.requestFocus()
            return false
        }

        if (selectedDate == 0L) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.tvSelectedStartTime.text == "Select Start Time") {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.tvSelectedEndTime.text == "Select End Time") {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (venueName.isEmpty()) {
            binding.etVenueName.error = "Venue name is required"
            binding.etVenueName.requestFocus()
            return false
        }

        return true
    }

    private fun saveEvent() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save event", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnSaveEvent.isEnabled = false
        binding.btnSaveEvent.text = "Saving..."
        binding.progressBar.visibility = View.VISIBLE

        val eventId = UUID.randomUUID().toString()
        val venue = EventVenue(
            name = binding.etVenueName.text.toString().trim(),
            address = binding.etVenueAddress.text.toString().trim(),
            city = binding.etVenueCity.text.toString().trim(),
            state = binding.etVenueState.text.toString().trim(),
            pincode = binding.etVenuePincode.text.toString().trim(),
            contactPerson = binding.etVenueContact.text.toString().trim(),
            contactPhone = binding.etVenuePhone.text.toString().trim(),
            capacity = binding.etVenueCapacity.text.toString().toIntOrNull() ?: 0
        )

        val event = Event(
            id = eventId,
            hostId = currentUser.uid,
            name = binding.etEventName.text.toString().trim(),
            description = binding.etEventDescription.text.toString().trim(),
            type = binding.spinnerEventType.selectedItem.toString().lowercase(),
            date = selectedDate,
            startTime = binding.tvSelectedStartTime.text.toString(),
            endTime = binding.tvSelectedEndTime.text.toString(),
            venue = venue,
            dresscode = binding.etDressCode.text.toString().trim(),
            notes = binding.etEventNotes.text.toString().trim(),
            requirements = binding.etSpecialRequirements.text.toString().trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                val result = eventRepository.addEvent(event)
                if (result.isSuccess) {
                    Toast.makeText(this@AddEventActivity, "Event '${event.name}' saved successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Event saved successfully: ${event.id}")
                    finish()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error saving event", error)
                    Toast.makeText(this@AddEventActivity, "Error saving event: ${error?.message}", Toast.LENGTH_LONG).show()
                    resetSaveButton()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving event", e)
                Toast.makeText(this@AddEventActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetSaveButton()
            }
        }
    }

    private fun resetSaveButton() {
        binding.btnSaveEvent.isEnabled = true
        binding.btnSaveEvent.text = "Save Event"
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
