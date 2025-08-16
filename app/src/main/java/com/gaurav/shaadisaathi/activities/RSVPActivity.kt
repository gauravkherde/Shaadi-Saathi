package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.RSVPAdapter
import com.gaurav.shaadisaathi.databinding.ActivityRsvpBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.GuestRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RSVPActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRsvpBinding
    private lateinit var rsvpAdapter: RSVPAdapter
    private val guestList = mutableListOf<Guest>() // FIX: Use Guest list
    private val guestRepository = GuestRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsvpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadGuests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "RSVP Management"
        }
    }

    private fun setupRecyclerView() {
        rsvpAdapter = RSVPAdapter(
            guests = guestList,
            onStatusUpdate = { guest: Guest, newStatus: String ->
                updateGuestRSVPStatus(guest, newStatus)
            },
            onSendReminder = { guest: Guest ->
                sendReminderToGuest(guest)
            }
        )

        binding.recyclerViewRSVP.apply {
            layoutManager = LinearLayoutManager(this@RSVPActivity)
            adapter = rsvpAdapter
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadGuests()
        }

        binding.fabSendReminders.setOnClickListener {
            sendBulkReminders()
        }
    }

    private fun loadGuests() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@RSVPActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val result = guestRepository.getAllGuests()
                if (result.isSuccess) {
                    val guests = result.getOrNull() ?: emptyList()
                    guestList.clear()
                    guestList.addAll(guests.sortedBy { it.name })
                    rsvpAdapter.notifyDataSetChanged()

                    updateStats()
                    updateEmptyState(guests.isEmpty())
                } else {
                    Toast.makeText(this@RSVPActivity, "Error loading guests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RSVPActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateGuestRSVPStatus(guest: Guest, newStatus: String) {
        lifecycleScope.launch {
            try {
                val updatedGuest = guest.copy(
                    rsvpStatus = newStatus,
                    rsvpResponseAt = if (newStatus != "pending") System.currentTimeMillis() else 0L,
                    updatedAt = System.currentTimeMillis()
                )

                val result = guestRepository.updateGuest(updatedGuest)
                if (result.isSuccess) {
                    // Update local list
                    val index = guestList.indexOfFirst { it.id == guest.id }
                    if (index != -1) {
                        guestList[index] = updatedGuest
                        rsvpAdapter.notifyItemChanged(index)
                    }

                    updateStats()
                    Toast.makeText(this@RSVPActivity, "RSVP status updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RSVPActivity, "Error updating RSVP status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RSVPActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendReminderToGuest(guest: Guest) {
        if (guest.rsvpStatus != "pending") {
            Toast.makeText(this, "${guest.name} has already responded", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = when {
            guest.phone.isNotEmpty() -> {
                // Send SMS reminder
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${guest.phone}")
                    putExtra("sms_body", "Hi ${guest.name}, this is a friendly reminder to RSVP for our wedding. Please let us know if you can attend. Thanks!")
                }
            }
            guest.email.isNotEmpty() -> {
                // Send email reminder
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${guest.email}")
                    putExtra(Intent.EXTRA_SUBJECT, "RSVP Reminder - Wedding Invitation")
                    putExtra(Intent.EXTRA_TEXT, "Dear ${guest.name},\n\nThis is a friendly reminder to RSVP for our wedding. We're excited to celebrate with you!\n\nPlease let us know if you can attend.\n\nThanks!")
                }
            }
            else -> {
                Toast.makeText(this, "No contact information available for ${guest.name}", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to send reminder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendBulkReminders() {
        val pendingGuests = guestList.filter { it.rsvpStatus == "pending" }

        if (pendingGuests.isEmpty()) {
            Toast.makeText(this, "No pending RSVPs to remind", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Send Bulk Reminders")
            .setMessage("Send RSVP reminders to ${pendingGuests.size} pending guests?")
            .setPositiveButton("Send") { _, _ ->
                performBulkReminders(pendingGuests)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBulkReminders(pendingGuests: List<Guest>) {
        // Create a combined message for all pending guests
        val phoneNumbers = pendingGuests.mapNotNull { guest ->
            if (guest.phone.isNotEmpty()) guest.phone else null
        }

        val emails = pendingGuests.mapNotNull { guest ->
            if (guest.email.isNotEmpty()) guest.email else null
        }

        // Show options to user
        val options = arrayOf("Send SMS to ${phoneNumbers.size} guests", "Send Email to ${emails.size} guests")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose Method")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sendBulkSMS(phoneNumbers)
                    1 -> sendBulkEmail(emails)
                }
            }
            .show()
    }

    private fun sendBulkSMS(phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(this, "No phone numbers available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${phoneNumbers.joinToString(";")}")
            putExtra("sms_body", "Hi! This is a friendly reminder to RSVP for our wedding. Please let us know if you can attend. Thanks!")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to send SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendBulkEmail(emails: List<String>) {
        if (emails.isEmpty()) {
            Toast.makeText(this, "No email addresses available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_BCC, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, "RSVP Reminder - Wedding Invitation")
            putExtra(Intent.EXTRA_TEXT, "Dear Friends,\n\nThis is a friendly reminder to RSVP for our wedding. We're excited to celebrate with you!\n\nPlease let us know if you can attend.\n\nThanks!")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to send email", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStats() {
        val totalGuests = guestList.size
        val confirmedGuests = guestList.count { it.rsvpStatus == "confirmed" }
        val pendingGuests = guestList.count { it.rsvpStatus == "pending" }
        val declinedGuests = guestList.count { it.rsvpStatus == "declined" }

        binding.apply {
            tvTotalGuests.text = totalGuests.toString()
            tvConfirmedCount.text = confirmedGuests.toString()
            tvPendingCount.text = pendingGuests.toString()
            tvDeclinedCount.text = declinedGuests.toString()

            // Update progress
            val responseRate = if (totalGuests > 0) {
                ((confirmedGuests + declinedGuests) * 100) / totalGuests
            } else {
                0
            }
            progressBarResponse.progress = responseRate
            tvResponseRate.text = "$responseRate%"
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewRSVP.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewRSVP.visibility = android.view.View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadGuests() // Refresh data when returning from other activities
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
