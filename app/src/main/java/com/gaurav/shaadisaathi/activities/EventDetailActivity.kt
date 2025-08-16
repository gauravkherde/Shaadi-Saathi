package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityEventDetailBinding
import com.gaurav.shaadisaathi.models.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val eventId = intent.getStringExtra("eventId")
        if (eventId != null) {
            loadEventDetails(eventId)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditEvent.setOnClickListener {
            currentEvent?.let { event ->
                val intent = Intent(this, AddEventActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            }
        }

        binding.btnDeleteEvent.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadEventDetails(eventId: String) {
        firestore.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                event?.let {
                    currentEvent = it
                    displayEventDetails(it)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading event: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayEventDetails(event: Event) {
        binding.tvEventName.text = event.name
        binding.tvEventType.text = event.getEventTypeDisplayName() // Use helper method

        // FIX: Format date and time properly
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(event.date))
        binding.tvEventDateTime.text = "$formattedDate at ${event.startTime}"

        // FIX: Access venue name from EventVenue object
        binding.tvEventVenue.text = event.venue.name

        // FIX: Access address from EventVenue object
        binding.tvEventAddress.text = event.venue.getFullAddress()

        binding.tvEventDescription.text = event.description
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent() {
        currentEvent?.let { event ->
            firestore.collection("events").document(event.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
