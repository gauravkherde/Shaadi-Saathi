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
import com.gaurav.shaadisaathi.databinding.ActivityEditEventBinding
import com.gaurav.shaadisaathi.models.Event
import com.gaurav.shaadisaathi.models.EventVenue
import com.gaurav.shaadisaathi.repository.EventRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEventBinding
    private lateinit var auth: FirebaseAuth
    private val eventRepository = EventRepository()
    private var currentEvent: Event? = null
    private var selectedDate: Long = 0L
    private val TAG = "EditEventActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupSpinners()
        setupClickListeners()
        loadEventData()

        Log.d(TAG, "EditEventActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Edit Event"
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

        binding.btnUpdateEvent.setOnClickListener {
            if (validateInput()) {
                updateEvent()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadEventData() {
        val eventId = intent.getStringExtra("eventId")
        if (eventId == null) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = eventRepository.getEvent(eventId)
                if (result.isSuccess) {
                    currentEvent = result.getOrNull()
                    currentEvent?.let { populateFields(it) }
                } else {
                    Toast.makeText(this@EditEventActivity, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading event", e)
                Toast.makeText(this@EditEventActivity, "Error loading event: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun populateFields(event: Event) {
        binding.etEventName.setText(event.name)
        binding.etEventDescription.setText(event.description)
        binding.etVenueName.setText(event.venue.name)
        binding.etVenueAddress.setText(event.venue.address)
        binding.etVenueCity.setText(event.venue.city)
        binding.etVenueState.setText(event.venue.state)
        binding.etVenuePincode.setText(event.venue.pincode)
        binding.etVenueCapacity.setText(if (event.venue.capacity > 0) event.venue.capacity.toString() else "")
        binding.etVenueContact.setText(event.venue.contactPerson)
        binding.etVenuePhone.setText(event.venue.contactPhone)
        binding.etDressCode.setText(event.dresscode)
        binding.etSpecialRequirements.setText(event.requirements)
        binding.etEventNotes.setText(event.notes)

        // Set event type spinner
        setSpinnerSelection(binding.spinnerEventType, event.type.replaceFirstChar { it.uppercase() })

        // Set date and time
        selectedDate = event.date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(Date(selectedDate))
        binding.tvSelectedDate.setTextColor(getColor(R.color.colorPrimary))

        binding.tvSelectedStartTime.text = event.startTime
        binding.tvSelectedStartTime.setTextColor(getColor(R.color.colorPrimary))

        binding.tvSelectedEndTime.text = event.endTime
        binding.tvSelectedEndTime.setTextColor(getColor(R.color.colorPrimary))

        Log.d(TAG, "Event data populated: ${event.name}")
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

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDate > 0) {
            calendar.timeInMillis = selectedDate
        }

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

    private fun updateEvent() {
        val event = currentEvent ?: return

        binding.btnUpdateEvent.isEnabled = false
        binding.btnUpdateEvent.text = "Updating..."
        binding.progressBar.visibility = View.VISIBLE

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

        val updatedEvent = event.copy(
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
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                val result = eventRepository.updateEvent(updatedEvent)
                if (result.isSuccess) {
                    Toast.makeText(this@EditEventActivity, "Event '${updatedEvent.name}' updated successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Event updated successfully: ${event.id}")
                    finish()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error updating event", error)
                    Toast.makeText(this@EditEventActivity, "Error updating event: ${error?.message}", Toast.LENGTH_LONG).show()
                    resetUpdateButton()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating event", e)
                Toast.makeText(this@EditEventActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetUpdateButton()
            }
        }
    }

    private fun resetUpdateButton() {
        binding.btnUpdateEvent.isEnabled = true
        binding.btnUpdateEvent.text = "Update Event"
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
