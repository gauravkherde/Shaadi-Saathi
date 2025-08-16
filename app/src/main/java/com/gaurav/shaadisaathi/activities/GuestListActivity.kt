package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.GuestListAdapter
import com.gaurav.shaadisaathi.databinding.ActivityGuestListBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.GuestRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class GuestListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestListBinding
    private lateinit var guestAdapter: GuestListAdapter
    private val guestList = mutableListOf<Guest>()
    private val filteredGuestList = mutableListOf<Guest>()
    private val guestRepository = GuestRepository()
    private val auth = FirebaseAuth.getInstance()

    private var currentCategoryFilter = "all"
    private var currentRSVPFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestListBinding.inflate(layoutInflater)
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
            title = "Guest List"
        }
    }

    private fun setupRecyclerView() {
        guestAdapter = GuestListAdapter(
            guests = filteredGuestList,
            onGuestClick = { guest: Guest ->
                openGuestDetail(guest)
            },
            onCallClick = { guest: Guest ->
                callGuest(guest)
            },
            onEmailClick = { guest: Guest ->
                emailGuest(guest)
            }
        )

        binding.recyclerViewGuests.apply {
            layoutManager = LinearLayoutManager(this@GuestListActivity)
            adapter = guestAdapter
        }
    }

    private fun setupClickListeners() {
        // SwipeRefresh listener
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadGuests()
        }

        // Add Guest FAB
        binding.fabAddGuest.setOnClickListener {
            val intent = Intent(this, AddGuestActivity::class.java)
            startActivity(intent)
        }

        // Category filter listeners
        binding.chipAll.setOnClickListener {
            filterGuestsByCategory("all")
            currentCategoryFilter = "all"
        }
        binding.chipFamily.setOnClickListener {
            filterGuestsByCategory("family")
            currentCategoryFilter = "family"
        }
        binding.chipFriends.setOnClickListener {
            filterGuestsByCategory("friends")
            currentCategoryFilter = "friends"
        }
        binding.chipColleagues.setOnClickListener {
            filterGuestsByCategory("colleagues")
            currentCategoryFilter = "colleagues"
        }

        // RSVP filter listeners
        binding.chipAllRSVP.setOnClickListener {
            filterGuestsByRSVP("all")
            currentRSVPFilter = "all"
        }
        binding.chipConfirmed.setOnClickListener {
            filterGuestsByRSVP("confirmed")
            currentRSVPFilter = "confirmed"
        }
        binding.chipPending.setOnClickListener {
            filterGuestsByRSVP("pending")
            currentRSVPFilter = "pending"
        }
        binding.chipDeclined.setOnClickListener {
            filterGuestsByRSVP("declined")
            currentRSVPFilter = "declined"
        }
    }

    private fun loadGuests() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@GuestListActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val result = guestRepository.getAllGuests()
                if (result.isSuccess) {
                    val guests = result.getOrNull() ?: emptyList()
                    guestList.clear()
                    guestList.addAll(guests.sortedBy { it.name })

                    // Apply current filters
                    applyFilters()

                    updateEmptyState(filteredGuestList.isEmpty())
                } else {
                    Toast.makeText(this@GuestListActivity, "Error loading guests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun filterGuestsByCategory(category: String) {
        currentCategoryFilter = category
        applyFilters()
    }

    private fun filterGuestsByRSVP(status: String) {
        currentRSVPFilter = status
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = guestList.toList()

        // Apply category filter
        if (currentCategoryFilter != "all") {
            filtered = filtered.filter { guest ->
                guest.category.lowercase() == currentCategoryFilter.lowercase()
            }
        }

        // Apply RSVP filter
        if (currentRSVPFilter != "all") {
            filtered = filtered.filter { guest ->
                guest.rsvpStatus.lowercase() == currentRSVPFilter.lowercase()
            }
        }

        filteredGuestList.clear()
        filteredGuestList.addAll(filtered)
        guestAdapter.updateGuests(filteredGuestList)

        updateEmptyState(filteredGuestList.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewGuests.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewGuests.visibility = android.view.View.VISIBLE
        }
    }

    private fun openGuestDetail(guest: Guest) {
        val intent = Intent(this, GuestDetailActivity::class.java)
        intent.putExtra("guestId", guest.id)
        startActivity(intent)
    }

    private fun callGuest(guest: Guest) {
        if (guest.phone.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${guest.phone}")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun emailGuest(guest: Guest) {
        if (guest.email.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${guest.email}")
                putExtra(Intent.EXTRA_SUBJECT, "Wedding Invitation")
                putExtra(Intent.EXTRA_TEXT, "You are cordially invited to our wedding!")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editGuest(guest: Guest) {
        val intent = Intent(this, EditGuestActivity::class.java)
        intent.putExtra("guestId", guest.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(guest: Guest) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Guest")
            .setMessage("Are you sure you want to delete ${guest.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGuest(guest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGuest(guest: Guest) {
        lifecycleScope.launch {
            try {
                val result = guestRepository.deleteGuest(guest.id)
                if (result.isSuccess) {
                    Toast.makeText(this@GuestListActivity, "Guest deleted successfully", Toast.LENGTH_SHORT).show()
                    loadGuests() // Refresh the list
                } else {
                    Toast.makeText(this@GuestListActivity, "Error deleting guest", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRSVPStatus(guest: Guest) {
        val statuses = arrayOf("Pending", "Confirmed", "Declined")
        val currentIndex = when (guest.rsvpStatus.lowercase()) {
            "confirmed" -> 1
            "declined" -> 2
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Update RSVP Status for ${guest.name}")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                val newStatus = statuses[which].lowercase()
                updateGuestRSVP(guest, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGuestRSVP(guest: Guest, newStatus: String) {
        lifecycleScope.launch {
            try {
                val updatedGuest = guest.copy(
                    rsvpStatus = newStatus,
                    rsvpResponseAt = if (newStatus != "pending") System.currentTimeMillis() else 0L,
                    updatedAt = System.currentTimeMillis()
                )

                val result = guestRepository.updateGuest(updatedGuest)
                if (result.isSuccess) {
                    Toast.makeText(this@GuestListActivity, "RSVP status updated", Toast.LENGTH_SHORT).show()
                    loadGuests() // Refresh the list
                } else {
                    Toast.makeText(this@GuestListActivity, "Error updating RSVP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GuestListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportGuestList() {
        lifecycleScope.launch {
            try {
                // Create CSV content
                val csvContent = StringBuilder()
                csvContent.append("Name,Category,Phone,Email,RSVP Status,Plus One\n")

                for (guest in guestList) {
                    csvContent.append("${guest.name},")
                    csvContent.append("${guest.category},")
                    csvContent.append("${guest.phone},")
                    csvContent.append("${guest.email},")
                    csvContent.append("${guest.rsvpStatus},")
                    csvContent.append("${if (guest.hasPlusOne) "Yes" else "No"}\n")
                }

                // Create share intent
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, csvContent.toString())
                    putExtra(Intent.EXTRA_SUBJECT, "Wedding Guest List")
                }

                startActivity(Intent.createChooser(intent, "Export Guest List"))

            } catch (e: Exception) {
                Toast.makeText(this@GuestListActivity, "Error exporting guest list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGuestStats() {
        val totalGuests = guestList.size
        val confirmedGuests = guestList.count { it.rsvpStatus == "confirmed" }
        val pendingGuests = guestList.count { it.rsvpStatus == "pending" }
        val declinedGuests = guestList.count { it.rsvpStatus == "declined" }

        // FIX: Alternative to sumOf for older Kotlin versions
        var totalWithPlusOnes = 0
        for (guest in guestList) {
            totalWithPlusOnes += if (guest.hasPlusOne && guest.plusOneConfirmed) 2 else 1
        }

        // Alternative using fold
        // val totalWithPlusOnes = guestList.fold(0) { acc, guest ->
        //     acc + if (guest.hasPlusOne && guest.plusOneConfirmed) 2 else 1
        // }

        val responseRate = if (totalGuests > 0) {
            ((confirmedGuests + declinedGuests) * 100) / totalGuests
        } else {
            0
        }

        val message = """
        Guest Statistics:
        
        Total Guests: $totalGuests
        Total Attendees (with +1): $totalWithPlusOnes
        
        RSVP Status:
        • Confirmed: $confirmedGuests
        • Pending: $pendingGuests  
        • Declined: $declinedGuests
        
        Response Rate: $responseRate%
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Guest Statistics")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


    private fun importContacts() {
        val intent = Intent(this, ImportContactsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_guest_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                importContacts()
                true
            }
            R.id.action_export -> {
                exportGuestList()
                true
            }
            R.id.action_stats -> {
                showGuestStats()
                true
            }
            R.id.action_filter -> {
                // Filter options are already handled by chips
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGuests() // Refresh the list when returning from other activities
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
