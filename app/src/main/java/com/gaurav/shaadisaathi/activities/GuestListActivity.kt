package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.GuestListAdapter
import com.gaurav.shaadisaathi.databinding.ActivityGuestListBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.utils.CSVExporter
import com.gaurav.shaadisaathi.utils.InvitationManager
import kotlinx.coroutines.launch

class GuestListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestListBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var guestAdapter: GuestListAdapter
    private val guestList = mutableListOf<Guest>()
    private val originalGuestList = mutableListOf<Guest>()
    private val TAG = "GuestListActivity"

    private var currentFilter = "all" // all, confirmed, pending, declined
    private var currentCategory = "all" // all, family, friends, colleagues, others
    private var additionalFilters = mutableSetOf<String>() // vip, plus_one, invitation_sent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadGuests()

        Log.d(TAG, "GuestListActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Guest Management"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        guestAdapter = GuestListAdapter(
            guests = guestList,
            onItemClick = { guest ->
                openGuestDetail(guest)
            },
            onEditClick = { guest ->
                editGuest(guest)
            },
            onDeleteClick = { guest ->
                showDeleteConfirmation(guest)
            },
            onRSVPClick = { guest ->
                updateRSVPStatus(guest)
            }
        )

        binding.recyclerViewGuests.apply {
            layoutManager = LinearLayoutManager(this@GuestListActivity)
            adapter = guestAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddGuest.setOnClickListener {
            val intent = Intent(this, AddGuestActivity::class.java)
            startActivity(intent)
        }

        binding.btnImportContacts.setOnClickListener {
            val intent = Intent(this, ImportContactsActivity::class.java)
            startActivity(intent)
        }

        // RSVP Status Filter Chips
        binding.chipAll.setOnClickListener { filterGuests("all") }
        binding.chipConfirmed.setOnClickListener { filterGuests("confirmed") }
        binding.chipPending.setOnClickListener { filterGuests("pending") }
        binding.chipDeclined.setOnClickListener { filterGuests("declined") }

        // Category Filter Chips
        binding.chipFamily.setOnClickListener { filterByCategory("family") }
        binding.chipFriends.setOnClickListener { filterByCategory("friends") }
        binding.chipColleagues.setOnClickListener { filterByCategory("colleagues") }
        binding.chipOthers.setOnClickListener { filterByCategory("others") }

        // Reset filters
        binding.chipAll.setOnLongClickListener {
            resetAllFilters()
            true
        }

        // Refresh gesture
        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadGuests()
        }
    }

    private fun loadGuests() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view guests", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout?.isRefreshing = true

        Log.d(TAG, "Loading guests for host: ${currentUser.uid}")

        firestore.collection("guests")
            .whereEqualTo("hostId", currentUser.uid)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (isFinishing || isDestroyed) {
                    Log.w(TAG, "Activity finishing/destroyed, ignoring Firestore callback")
                    return@addSnapshotListener
                }

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout?.isRefreshing = false

                    if (e != null) {
                        Log.e(TAG, "Error loading guests", e)

                        when {
                            e.message?.contains("PERMISSION_DENIED") == true -> {
                                showFirestorePermissionErrorDialog()
                            }
                            e.message?.contains("permission") == true -> {
                                Toast.makeText(this@GuestListActivity, "Permission denied. Check Firestore rules.", Toast.LENGTH_LONG).show()
                            }
                            e.message?.contains("Missing or insufficient permissions") == true -> {
                                showFirestorePermissionErrorDialog()
                            }
                            else -> {
                                Toast.makeText(this@GuestListActivity, "Error loading guests: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return@runOnUiThread
                    }

                    originalGuestList.clear()
                    val validGuests = mutableListOf<Guest>()

                    snapshots?.documents?.forEach { doc ->
                        try {
                            val guest = doc.toObject(Guest::class.java)
                            guest?.let {
                                validGuests.add(it)
                                Log.d(TAG, "Loaded guest: ${it.name}")
                            }
                        } catch (ex: Exception) {
                            Log.w(TAG, "Skipping invalid guest document: ${doc.id}")
                        }
                    }

                    validGuests.sortBy { it.name.lowercase() }
                    originalGuestList.addAll(validGuests)

                    // Apply current filters
                    applyCurrentFilters()
                    updateStats()

                    Log.d(TAG, "Total guests loaded: ${originalGuestList.size}")
                }
            }
    }

    private fun showFirestorePermissionErrorDialog() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing/destroyed, cannot show Firestore permission dialog")
            return
        }

        try {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this)
                        .setTitle("Firestore Permission Error")
                        .setMessage("Cannot access wedding guests due to database security rules.\n\nThis means:\n• Firestore rules need updating\n• User authentication may have expired\n\nPlease check Firebase Console → Firestore Database → Rules")
                        .setPositiveButton("Retry") { _, _ ->
                            if (!isFinishing && !isDestroyed) {
                                loadGuests()
                            }
                        }
                        .setNegativeButton("Close") { _, _ ->
                            if (!isFinishing && !isDestroyed) {
                                finish()
                            }
                        }
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Firestore permission dialog", e)
        }
    }

    private fun filterGuests(status: String) {
        currentFilter = status
        updateChipSelection()
        applyCurrentFilters()
    }

    private fun filterByCategory(category: String) {
        currentCategory = category
        updateCategoryChipSelection()
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        var filteredList = originalGuestList.toList()

        // Apply RSVP status filter
        if (currentFilter != "all") {
            filteredList = filteredList.filter { guest ->
                guest.rsvpStatus == currentFilter
            }
        }

        // Apply category filter
        if (currentCategory != "all") {
            filteredList = filteredList.filter { guest ->
                guest.category == currentCategory
            }
        }

        // Apply additional filters
        additionalFilters.forEach { filter ->
            filteredList = when (filter) {
                "vip" -> filteredList.filter { it.isVip }
                "plus_one" -> filteredList.filter { it.hasPlusOne }
                "invitation_sent" -> filteredList.filter { it.invitationSent }
                else -> filteredList
            }
        }

        // Update the displayed list
        guestList.clear()
        guestList.addAll(filteredList)
        guestAdapter.notifyDataSetChanged()
        updateEmptyState(filteredList.isEmpty())

        Log.d(TAG, "Applied filters - showing ${filteredList.size} out of ${originalGuestList.size} guests")
    }

    private fun updateChipSelection() {
        // Reset all RSVP chips
        binding.chipAll.isChecked = false
        binding.chipConfirmed.isChecked = false
        binding.chipPending.isChecked = false
        binding.chipDeclined.isChecked = false

        // Set selected chip
        when (currentFilter) {
            "all" -> binding.chipAll.isChecked = true
            "confirmed" -> binding.chipConfirmed.isChecked = true
            "pending" -> binding.chipPending.isChecked = true
            "declined" -> binding.chipDeclined.isChecked = true
        }
    }

    private fun updateCategoryChipSelection() {
        binding.chipFamily.isChecked = currentCategory == "family"
        binding.chipFriends.isChecked = currentCategory == "friends"
        binding.chipColleagues.isChecked = currentCategory == "colleagues"
        binding.chipOthers.isChecked = currentCategory == "others"
    }

    private fun resetAllFilters() {
        currentFilter = "all"
        currentCategory = "all"
        additionalFilters.clear()
        updateChipSelection()
        updateCategoryChipSelection()
        applyCurrentFilters()
        Toast.makeText(this, "All filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateStats() {
        val totalGuests = originalGuestList.size
        val confirmedCount = originalGuestList.count { it.rsvpStatus == "confirmed" }
        val pendingCount = originalGuestList.count { it.rsvpStatus == "pending" }
        val declinedCount = originalGuestList.count { it.rsvpStatus == "declined" }
        val totalAttending = originalGuestList.filter { it.rsvpStatus == "confirmed" }
            .sumOf { it.getTotalGuests() }

        binding.tvTotalGuests.text = "Total: $totalGuests"
        binding.tvConfirmedCount.text = "Confirmed: $confirmedCount"
        binding.tvPendingCount.text = "Pending: $pendingCount"
        binding.tvDeclinedCount.text = "Declined: $declinedCount"
        binding.tvTotalAttending.text = "Attending: $totalAttending"

        // Update chip badges with counts
        binding.chipConfirmed.text = "Confirmed ($confirmedCount)"
        binding.chipPending.text = "Pending ($pendingCount)"
        binding.chipDeclined.text = "Declined ($declinedCount)"

        // Update category counts
        val familyCount = originalGuestList.count { it.category == "family" }
        val friendsCount = originalGuestList.count { it.category == "friends" }
        val colleaguesCount = originalGuestList.count { it.category == "colleagues" }
        val othersCount = originalGuestList.count { it.category == "others" }

        binding.chipFamily.text = "Family ($familyCount)"
        binding.chipFriends.text = "Friends ($friendsCount)"
        binding.chipColleagues.text = "Colleagues ($colleaguesCount)"
        binding.chipOthers.text = "Others ($othersCount)"
    }

    private fun openGuestDetail(guest: Guest) {
        val intent = Intent(this, GuestDetailActivity::class.java)
        intent.putExtra("guestId", guest.id)
        startActivity(intent)
    }

    private fun editGuest(guest: Guest) {
        val intent = Intent(this, EditGuestActivity::class.java)
        intent.putExtra("guestId", guest.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(guest: Guest) {
        AlertDialog.Builder(this)
            .setTitle("Delete Guest")
            .setMessage("Are you sure you want to delete ${guest.name}?\n\nThis action cannot be undone and will remove all associated data including RSVP status and notes.")
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
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting guest", e)
                Toast.makeText(this, "Error deleting guest: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
            .setTitle("Update RSVP Status for ${guest.name}")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val newStatus = when (which) {
                    0 -> "confirmed"
                    1 -> "pending"
                    2 -> "declined"
                    else -> "pending"
                }

                updateGuestRSVP(guest, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGuestRSVP(guest: Guest, newStatus: String) {
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
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating RSVP", e)
                Toast.makeText(this, "Error updating RSVP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewGuests.visibility = View.GONE

            val message = when {
                currentFilter != "all" && currentCategory != "all" ->
                    "No guests found with current filters\n\n${currentFilter.replaceFirstChar { it.uppercase() }} guests in ${currentCategory.replaceFirstChar { it.uppercase() }} category"
                currentFilter != "all" ->
                    "No ${currentFilter} guests found"
                currentCategory != "all" ->
                    "No guests found in ${currentCategory.replaceFirstChar { it.uppercase() }} category"
                additionalFilters.isNotEmpty() ->
                    "No guests match the selected filters"
                originalGuestList.isEmpty() ->
                    "No guests added yet.\n\nStart by adding your first guest or importing from contacts!"
                else ->
                    "No guests match your current filters"
            }

            binding.tvEmptyMessage.text = message
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewGuests.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_guest_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_export_csv -> {
                exportGuestList()
                true
            }
            R.id.action_send_invitations -> {
                sendBulkInvitations()
                true
            }
            R.id.action_guest_stats -> {
                showGuestStatistics()
                true
            }
            R.id.action_filter -> {
                showAdvancedFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportGuestList() {
        if (originalGuestList.isEmpty()) {
            Toast.makeText(this, "No guests to export", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val result = CSVExporter.exportGuestsToCSV(this@GuestListActivity, originalGuestList)
                if (result.isSuccess) {
                    val uri = result.getOrNull()
                    uri?.let {
                        CSVExporter.shareCSV(this@GuestListActivity, it, "wedding_guests.csv")
                        Toast.makeText(this@GuestListActivity, "Guest list exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@GuestListActivity, "Error exporting CSV: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting guest list", e)
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendBulkInvitations() {
        val pendingGuests = originalGuestList.filter { !it.invitationSent && it.rsvpStatus == "pending" && it.email.isNotEmpty() }
        if (pendingGuests.isEmpty()) {
            Toast.makeText(this, "No pending invitations to send.\n\nMake sure guests have email addresses and haven't received invitations yet.", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Send Bulk Invitations")
            .setMessage("Send invitations to ${pendingGuests.size} guests?\n\nThis will send digital invitations via email to all guests who:\n• Haven't received an invitation yet\n• Have RSVP status 'Pending'\n• Have email addresses")
            .setPositiveButton("Send Invitations") { _, _ ->
                performBulkInvitationSending(pendingGuests)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBulkInvitationSending(pendingGuests: List<Guest>) {
        lifecycleScope.launch {
            try {
                // Create wedding invitation content
                val eventDetails = createWeddingInvitationContent()

                val result = InvitationManager.sendBulkInvitations(
                    this@GuestListActivity,
                    pendingGuests,
                    eventDetails
                )

                if (result.isSuccess) {
                    val sentCount = result.getOrDefault(0)

                    AlertDialog.Builder(this@GuestListActivity)
                        .setTitle("Invitations Sent!")
                        .setMessage("Successfully sent invitations to $sentCount guests!\n\nGuests will receive email invitations and their invitation status will be updated.")
                        .setPositiveButton("OK") { _, _ ->
                            loadGuests() // Refresh to show updated invitation status
                        }
                        .show()
                } else {
                    Toast.makeText(this@GuestListActivity, "Error sending invitations: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending bulk invitations", e)
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createWeddingInvitationContent(): String {
        return """
            🎉 You're Invited to Our Wedding! 🎉
            
            We are delighted to invite you to celebrate our special day with us.
            
            📅 Date: [Your Wedding Date]
            🕐 Time: [Wedding Time]
            📍 Venue: [Wedding Venue]
            
            Your presence will make our day even more meaningful and joyous.
            
            Please RSVP by [RSVP Date] by responding to this email or contacting us directly.
            
            We can't wait to celebrate with you!
            
            With love and excitement,
            [Bride & Groom Names]
            
            ---
            This invitation was sent through ShaadiSaathi Wedding App
        """.trimIndent()
    }

    private fun showGuestStatistics() {
        // If you have a separate GuestStatsActivity
        val intent = Intent(this, GuestStatsActivity::class.java)
        startActivity(intent)

        // Alternative: Show statistics in a dialog
        // showStatisticsDialog()
    }

    private fun showStatisticsDialog() {
        val stats = calculateGuestStatistics()

        val message = """
            📊 Guest Statistics
            
            Total Guests: ${stats.totalGuests}
            Total Attending: ${stats.totalAttending}
            
            RSVP Breakdown:
            • Confirmed: ${stats.confirmedGuests}
            • Pending: ${stats.pendingGuests} 
            • Declined: ${stats.declinedGuests}
            
            Category Breakdown:
            • Family: ${stats.familyCount}
            • Friends: ${stats.friendsCount}
            • Colleagues: ${stats.colleaguesCount}
            • Others: ${stats.othersCount}
            
            Additional Info:
            • VIP Guests: ${stats.vipGuests}
            • Plus One Guests: ${stats.plusOneGuests}
            • Invitations Sent: ${stats.invitationsSent}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Guest Statistics")
            .setMessage(message)
            .setPositiveButton("Export Statistics") { _, _ ->
                exportStatistics()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun calculateGuestStatistics(): GuestStats {
        return GuestStats(
            totalGuests = originalGuestList.size,
            confirmedGuests = originalGuestList.count { it.rsvpStatus == "confirmed" },
            pendingGuests = originalGuestList.count { it.rsvpStatus == "pending" },
            declinedGuests = originalGuestList.count { it.rsvpStatus == "declined" },
            totalAttending = originalGuestList.filter { it.rsvpStatus == "confirmed" }.sumOf { it.getTotalGuests() },
            familyCount = originalGuestList.count { it.category == "family" },
            friendsCount = originalGuestList.count { it.category == "friends" },
            colleaguesCount = originalGuestList.count { it.category == "colleagues" },
            othersCount = originalGuestList.count { it.category == "others" },
            vipGuests = originalGuestList.count { it.isVip },
            plusOneGuests = originalGuestList.count { it.hasPlusOne },
            invitationsSent = originalGuestList.count { it.invitationSent }
        )
    }

    data class GuestStats(
        val totalGuests: Int,
        val confirmedGuests: Int,
        val pendingGuests: Int,
        val declinedGuests: Int,
        val totalAttending: Int,
        val familyCount: Int,
        val friendsCount: Int,
        val colleaguesCount: Int,
        val othersCount: Int,
        val vipGuests: Int,
        val plusOneGuests: Int,
        val invitationsSent: Int
    )

    private fun exportStatistics() {
        lifecycleScope.launch {
            try {
                val result = CSVExporter.exportGuestStatisticsToCSV(this@GuestListActivity, originalGuestList)
                if (result.isSuccess) {
                    val uri = result.getOrNull()
                    uri?.let {
                        CSVExporter.shareCSV(this@GuestListActivity, it, "guest_statistics.csv")
                        Toast.makeText(this@GuestListActivity, "Statistics exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@GuestListActivity, "Error exporting statistics", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting statistics", e)
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAdvancedFilterDialog() {
        // Create and show advanced filter dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_guest_filter, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Setup filter dialog interactions
        // This would require additional implementation based on dialog_guest_filter.xml

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other activities
        if (::guestAdapter.isInitialized) {
            loadGuests()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "GuestListActivity being destroyed")

        try {
            // Cancel any pending operations or listeners
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }

        super.onDestroy()
    }
}
