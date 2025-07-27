package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityGuestDetailBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.utils.CalendarIntegration
import com.gaurav.shaadisaathi.utils.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.*

class GuestDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestDetailBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var currentGuest: Guest? = null
    private val TAG = "GuestDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupClickListeners()
        loadGuestData()

        Log.d(TAG, "GuestDetailActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Guest Details"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnCallGuest.setOnClickListener {
            currentGuest?.let { callGuest(it) }
        }

        binding.btnEmailGuest.setOnClickListener {
            currentGuest?.let { emailGuest(it) }
        }

        binding.btnSendInvitation.setOnClickListener {
            currentGuest?.let { sendInvitation(it) }
        }

        binding.btnUpdateRsvp.setOnClickListener {
            currentGuest?.let { updateRSVPStatus(it) }
        }

        binding.btnScheduleReminder.setOnClickListener {
            currentGuest?.let { scheduleReminder(it) }
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
                    currentGuest?.let { displayGuestDetails(it) }
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

    private fun displayGuestDetails(guest: Guest) {
        // Update toolbar title
        supportActionBar?.title = guest.name

        // Guest header
        createGuestDetailHeader(guest)

        // Basic Information
        binding.tvGuestName.text = guest.name
        binding.tvGuestCategory.text = guest.getCategoryDisplayName()
        binding.tvRelationToHost.text = if (guest.relationToHost.isNotEmpty()) guest.relationToHost else "Not specified"

        // Contact Information
        if (guest.phone.isNotEmpty()) {
            binding.layoutPhone.visibility = View.VISIBLE
            binding.tvGuestPhone.text = guest.phone
        } else {
            binding.layoutPhone.visibility = View.GONE
        }

        if (guest.email.isNotEmpty()) {
            binding.layoutEmail.visibility = View.VISIBLE
            binding.tvGuestEmail.text = guest.email
        } else {
            binding.layoutEmail.visibility = View.GONE
        }

        if (guest.address.isNotEmpty()) {
            binding.layoutAddress.visibility = View.VISIBLE
            binding.tvGuestAddress.text = guest.address
        } else {
            binding.layoutAddress.visibility = View.GONE
        }

        // RSVP Status
        updateRSVPDisplay(guest)

        // Preferences
        binding.tvMealPreference.text = guest.getMealPreferenceDisplayName()
        binding.tvInvitationPreference.text = guest.invitationPreference.replaceFirstChar { it.uppercase() }
        binding.tvLanguagePreference.text = guest.languagePreference.replaceFirstChar { it.uppercase() }

        // Plus One Information
        if (guest.hasPlusOne) {
            binding.layoutPlusOne.visibility = View.VISIBLE
            binding.tvPlusOneName.text = if (guest.plusOneName.isNotEmpty()) guest.plusOneName else "Not specified"
            binding.tvPlusOneStatus.text = if (guest.plusOneConfirmed) "Confirmed" else "Pending"
            binding.tvPlusOneStatus.setTextColor(
                ContextCompat.getColor(this, if (guest.plusOneConfirmed) R.color.status_confirmed else R.color.status_pending)
            )
        } else {
            binding.layoutPlusOne.visibility = View.GONE
        }

        // Additional Information
        if (guest.specialRequirements.isNotEmpty()) {
            binding.layoutSpecialRequirements.visibility = View.VISIBLE
            binding.tvSpecialRequirements.text = guest.specialRequirements
        } else {
            binding.layoutSpecialRequirements.visibility = View.GONE
        }

        if (guest.notes.isNotEmpty()) {
            binding.layoutNotes.visibility = View.VISIBLE
            binding.tvNotes.text = guest.notes
        } else {
            binding.layoutNotes.visibility = View.GONE
        }

        if (guest.tableNumber > 0) {
            binding.layoutTableNumber.visibility = View.VISIBLE
            binding.tvTableNumber.text = "Table ${guest.tableNumber}"
        } else {
            binding.layoutTableNumber.visibility = View.GONE
        }

        // Special Indicators
        binding.chipVip.visibility = if (guest.isVip) View.VISIBLE else View.GONE
        binding.chipCanUploadPhotos.visibility = if (guest.canUploadPhotos) View.VISIBLE else View.GONE
        binding.chipInvitationSent.visibility = if (guest.invitationSent) View.VISIBLE else View.GONE

        // Action button states
        updateActionButtons(guest)

        // Timestamps
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        binding.tvCreatedAt.text = "Added: ${dateFormat.format(Date(guest.createdAt))}"
        if (guest.updatedAt > guest.createdAt) {
            binding.tvUpdatedAt.visibility = View.VISIBLE
            binding.tvUpdatedAt.text = "Last updated: ${dateFormat.format(Date(guest.updatedAt))}"
        } else {
            binding.tvUpdatedAt.visibility = View.GONE
        }

        if (guest.rsvpResponseAt > 0) {
            binding.tvRsvpResponseAt.visibility = View.VISIBLE
            binding.tvRsvpResponseAt.text = "RSVP received: ${dateFormat.format(Date(guest.rsvpResponseAt))}"
        } else {
            binding.tvRsvpResponseAt.visibility = View.GONE
        }

        Log.d(TAG, "Guest details displayed: ${guest.name}")
    }

    private fun createGuestDetailHeader(guest: Guest) {
        // Update guest initial display
        val initial = if (guest.name.isNotEmpty()) {
            guest.name.first().toString().uppercase()
        } else {
            "G"
        }
        binding.tvGuestInitial.text = initial

        // Set background color based on category
        val backgroundColor = when (guest.category) {
            "family" -> R.color.category_family
            "friends" -> R.color.category_friends
            "colleagues" -> R.color.category_colleagues
            else -> R.color.colorPrimary
        }
        binding.tvGuestInitial.setBackgroundResource(backgroundColor)
    }

    private fun updateRSVPDisplay(guest: Guest) {
        binding.tvGuestRsvpStatus.text = guest.rsvpStatus.replaceFirstChar { it.uppercase() }

        val (statusColor, statusBackground) = when (guest.rsvpStatus) {
            "confirmed" -> Pair(R.color.status_confirmed, R.drawable.bg_status_confirmed)
            "declined" -> Pair(R.color.status_declined, R.drawable.bg_status_declined)
            else -> Pair(R.color.status_pending, R.drawable.bg_status_pending)
        }

        binding.tvGuestRsvpStatus.setTextColor(ContextCompat.getColor(this, statusColor))
        binding.tvGuestRsvpStatus.setBackgroundResource(statusBackground)

        // Attending count
        val attendingText = when {
            guest.rsvpStatus == "confirmed" -> {
                val count = guest.getTotalGuests()
                "$count person${if (count > 1) "s" else ""} attending"
            }
            guest.rsvpStatus == "declined" -> "Not attending"
            else -> "Response pending"
        }
        binding.tvAttendingCount.text = attendingText
    }

    private fun updateActionButtons(guest: Guest) {
        // Call button
        binding.btnCallGuest.isEnabled = guest.phone.isNotEmpty()

        // Email button
        binding.btnEmailGuest.isEnabled = guest.email.isNotEmpty()

        // Send invitation button
        binding.btnSendInvitation.isEnabled = guest.email.isNotEmpty() && !guest.invitationSent
        binding.btnSendInvitation.text = if (guest.invitationSent) "Invitation Sent" else "Send Invitation"

        // Update RSVP button
        binding.btnUpdateRsvp.text = when (guest.rsvpStatus) {
            "confirmed" -> "Change RSVP"
            "declined" -> "Change RSVP"
            else -> "Update RSVP"
        }
    }

    private fun callGuest(guest: Guest) {
        if (guest.phone.isEmpty()) {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${guest.phone}")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun emailGuest(guest: Guest) {
        if (guest.email.isEmpty()) {
            Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:${guest.email}")
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wedding Invitation - ${guest.name}")
            intent.putExtra(Intent.EXTRA_TEXT, createEmailBody(guest))
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email", e)
            Toast.makeText(this, "Unable to send email", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createEmailBody(guest: Guest): String {
        return """
            Dear ${guest.name},
            
            We hope this message finds you well!
            
            We are delighted to invite you to our wedding celebration. Your presence would make our special day even more meaningful.
            
            Please let us know if you can attend by responding to this email or contacting us directly.
            
            Thank you and we look forward to celebrating with you!
            
            With love,
            [Your Names]
        """.trimIndent()
    }

    private fun sendInvitation(guest: Guest) {
        if (guest.email.isEmpty()) {
            Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show()
            return
        }

        if (guest.invitationSent) {
            Toast.makeText(this, "Invitation already sent to ${guest.name}", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Send Invitation")
            .setMessage("Send wedding invitation to ${guest.name}?\n\nEmail: ${guest.email}")
            .setPositiveButton("Send") { _, _ ->
                performSendInvitation(guest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSendInvitation(guest: Guest) {
        // Update invitation sent status
        val updates = mapOf(
            "invitationSent" to true,
            "invitationSentAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("guests").document(guest.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Invitation sent to ${guest.name}!", Toast.LENGTH_SHORT).show()

                // Send actual email invitation
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:${guest.email}")
                intent.putExtra(Intent.EXTRA_SUBJECT, "🎉 You're Invited to Our Wedding!")
                intent.putExtra(Intent.EXTRA_TEXT, createDetailedInvitationEmail(guest))
                startActivity(Intent.createChooser(intent, "Send Wedding Invitation"))

                // Refresh display
                loadGuestData()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating invitation status", e)
                Toast.makeText(this, "Error sending invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createDetailedInvitationEmail(guest: Guest): String {
        return """
            🎉 You're Invited to Our Wedding! 🎉
            
            Dear ${guest.name},
            
            We are thrilled to invite you to celebrate our special day with us!
            
            📅 Date: [Your Wedding Date]
            🕐 Time: [Wedding Time]
            📍 Venue: [Wedding Venue Address]
            
            Your presence will make our celebration complete and filled with joy.
            
            Please RSVP by [RSVP Date] by replying to this email or contacting us at [Your Contact].
            
            ${if (guest.hasPlusOne) "We've reserved a seat for your plus one as well!" else ""}
            
            ${if (guest.specialRequirements.isNotEmpty()) "Special arrangements noted: ${guest.specialRequirements}" else ""}
            
            We can't wait to celebrate with you!
            
            With love and excitement,
            [Bride & Groom Names]
            
            ---
            This invitation was sent through ShaadiSaathi Wedding App
        """.trimIndent()
    }

    private fun updateRSVPStatus(guest: Guest) {
        val options = arrayOf("Confirmed", "Pending", "Declined")
        val currentIndex = when (guest.rsvpStatus) {
            "confirmed" -> 0
            "pending" -> 1
            "declined" -> 2
            else -> 1
        }

        AlertDialog.Builder(this)
            .setTitle("Update RSVP Status")
            .setMessage("Change RSVP status for ${guest.name}")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val newStatus = when (which) {
                    0 -> "confirmed"
                    1 -> "pending"
                    2 -> "declined"
                    else -> "pending"
                }

                performRSVPUpdate(guest, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRSVPUpdate(guest: Guest, newStatus: String) {
        val updates = mapOf(
            "rsvpStatus" to newStatus,
            "rsvpResponseAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("guests").document(guest.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "RSVP updated for ${guest.name}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "RSVP updated: ${guest.id} -> $newStatus")
                loadGuestData() // Refresh display
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating RSVP", e)
                Toast.makeText(this, "Error updating RSVP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleReminder(guest: Guest) {
        val options = arrayOf(
            "Follow-up in 3 days",
            "Follow-up in 1 week",
            "RSVP deadline reminder",
            "Custom reminder"
        )

        AlertDialog.Builder(this)
            .setTitle("Schedule Reminder")
            .setMessage("Set a reminder for ${guest.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> scheduleReminderWithDelay(guest, 3 * 24 * 60 * 60 * 1000L) // 3 days
                    1 -> scheduleReminderWithDelay(guest, 7 * 24 * 60 * 60 * 1000L) // 1 week
                    2 -> scheduleRSVPDeadlineReminder(guest)
                    3 -> showCustomReminderDialog(guest)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleReminderWithDelay(guest: Guest, delayMillis: Long) {
        val reminderTime = System.currentTimeMillis() + delayMillis
        NotificationScheduler.scheduleRSVPReminder(
            this,
            guest.name,
            "Wedding Follow-up",
            reminderTime,
            guest.id.hashCode()
        )

        val days = delayMillis / (24 * 60 * 60 * 1000)
        Toast.makeText(this, "Reminder set for ${guest.name} in $days days", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleRSVPDeadlineReminder(guest: Guest) {
        // Schedule reminder 1 week before wedding (assuming wedding is 1 month away)
        val reminderTime = System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000) // 3 weeks
        NotificationScheduler.scheduleRSVPReminder(
            this,
            guest.name,
            "RSVP Deadline Approaching",
            reminderTime,
            guest.id.hashCode()
        )

        Toast.makeText(this, "RSVP deadline reminder set for ${guest.name}", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleRSVPReminder(guest: Guest) {
        if (guest.rsvpStatus == "pending") {
            val reminderTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
            NotificationScheduler.scheduleRSVPReminder(
                this,
                guest.name,
                "Wedding Celebration",
                reminderTime,
                guest.id.hashCode()
            )
        }
    }

    private fun showCustomReminderDialog(guest: Guest) {
        // TODO: Implement custom reminder dialog with date/time picker
        Toast.makeText(this, "Custom reminder - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun addToCalendar(guest: Guest) {
        // Add guest's important dates to calendar
        CalendarIntegration.addEventToCalendar(
            this,
            "RSVP Follow-up - ${guest.name}",
            "Follow up on wedding RSVP from ${guest.name}",
            "",
            System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000), // 3 days from now
            System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000) + (60 * 60 * 1000), // 1 hour duration
            false
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_guest_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_edit_guest -> {
                currentGuest?.let { guest ->
                    val intent = Intent(this, EditGuestActivity::class.java)
                    intent.putExtra("guestId", guest.id)
                    startActivity(intent)
                }
                true
            }
            R.id.action_delete_guest -> {
                currentGuest?.let { guest ->
                    showDeleteConfirmation(guest)
                }
                true
            }
            R.id.action_share_guest -> {
                currentGuest?.let { guest ->
                    shareGuestInfo(guest)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmation(guest: Guest) {
        AlertDialog.Builder(this)
            .setTitle("Delete Guest")
            .setMessage("Are you sure you want to delete ${guest.name}?\n\nThis action cannot be undone and will remove all associated data.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGuest(guest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGuest(guest: Guest) {
        firestore.collection("guests").document(guest.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${guest.name} deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Guest deleted: ${guest.id}")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting guest", e)
                Toast.makeText(this, "Error deleting guest: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun shareGuestInfo(guest: Guest) {
        val shareText = """
            Guest Information:
            Name: ${guest.name}
            Category: ${guest.getCategoryDisplayName()}
            RSVP Status: ${guest.rsvpStatus.replaceFirstChar { it.uppercase() }}
            ${if (guest.phone.isNotEmpty()) "Phone: ${guest.phone}" else ""}
            ${if (guest.email.isNotEmpty()) "Email: ${guest.email}" else ""}
            ${if (guest.hasPlusOne) "Plus One: ${guest.plusOneName}" else ""}
            
            Shared via ShaadiSaathi Wedding App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Guest Information - ${guest.name}")
        intent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(intent, "Share Guest Info"))
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from edit activity
        loadGuestData()
    }
}
