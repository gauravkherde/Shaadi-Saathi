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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivityMainBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.models.Event
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.repository.GuestRepository
import com.gaurav.shaadisaathi.repository.EventRepository
import com.gaurav.shaadisaathi.repository.VendorRepository
import com.gaurav.shaadisaathi.utils.NotificationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val guestRepository = GuestRepository()
    private val eventRepository = EventRepository()
    private val vendorRepository = VendorRepository()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Initialize notification channels
        NotificationUtils.createNotificationChannels(this)

        setupToolbar()
        setupClickListeners()
        checkUserAuthentication()
        loadDashboardData()

        Log.d(TAG, "MainActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "ShaadiSaathi"
            subtitle = "Your Wedding Companion"
        }
    }

    private fun setupClickListeners() {
        // Phase 4 Navigation - Guest Management
        binding.cardGuestManagement.setOnClickListener {
            val intent = Intent(this, GuestListActivity::class.java)
            startActivity(intent)
        }

        // Phase 4 Navigation - Event Timeline
        binding.cardEventTimeline.setOnClickListener {
            val intent = Intent(this, EventTimelineActivity::class.java)
            startActivity(intent)
        }

        // Phase 4 Navigation - Vendor Directory
        binding.cardVendorDirectory.setOnClickListener {
            val intent = Intent(this, VendorDirectoryActivity::class.java)
            startActivity(intent)
        }

        // Other Phase Navigation
        binding.cardPhotoGallery.setOnClickListener {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            startActivity(intent)
        }

        binding.cardChatRooms.setOnClickListener {
            val intent = Intent(this, CreateChatRoomActivity::class.java)
            startActivity(intent)
        }

        binding.cardBudgetTracker.setOnClickListener {
            // TODO: Implement Budget Tracker in future phases
            showFeatureComingSoon("Budget Tracker")
        }

        binding.cardWeddingChecklist.setOnClickListener {
            // TODO: Implement Wedding Checklist in future phases
            showFeatureComingSoon("Wedding Checklist")
        }

        binding.cardInvitationDesigner.setOnClickListener {
            // TODO: Implement Invitation Designer in future phases
            showFeatureComingSoon("Invitation Designer")
        }

        // Quick Action Buttons
        binding.btnQuickAddGuest.setOnClickListener {
            val intent = Intent(this, AddGuestActivity::class.java)
            startActivity(intent)
        }

        binding.btnQuickAddEvent.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }

        binding.btnQuickImportContacts.setOnClickListener {
            val intent = Intent(this, ImportContactsActivity::class.java)
            startActivity(intent)
        }

        binding.btnQuickExportGuests.setOnClickListener {
            showExportOptions()
        }

        // Profile and Settings
        binding.ivUserProfile.setOnClickListener {
            showUserProfileDialog()
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Wedding Countdown
        binding.cardWeddingCountdown.setOnClickListener {
            showSetWeddingDateDialog()
        }

        // Refresh Dashboard
        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadDashboardData()
        }
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Redirect to login if not authenticated
            redirectToLogin()
            return
        }

        // Display user info
        binding.tvUserName.text = currentUser.displayName ?: "Wedding Planner"
        binding.tvUserEmail.text = currentUser.email ?: ""

        // Set user profile image initial
        val initial = currentUser.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U"
        binding.tvUserInitial.text = initial

        Log.d(TAG, "User authenticated: ${currentUser.uid}")
    }

    private fun loadDashboardData() {
        binding.progressBarDashboard?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load all dashboard data concurrently
                loadGuestStats()
                loadEventStats()
                loadVendorStats()
                loadWeddingCountdown()
                loadRecentActivity()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dashboard data", e)
                Toast.makeText(this@MainActivity, "Error loading dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarDashboard?.visibility = View.GONE
                binding.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private suspend fun loadGuestStats() {
        try {
            val result = guestRepository.getGuestStatistics()
            if (result.isSuccess) {
                val stats = result.getOrNull()
                stats?.let { updateGuestStatsUI(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading guest stats", e)
        }
    }

    private fun updateGuestStatsUI(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        binding.tvTotalGuests.text = stats.totalGuests.toString()
        binding.tvConfirmedGuests.text = stats.confirmedGuests.toString()
        binding.tvPendingGuests.text = stats.pendingGuests.toString()
        binding.tvTotalAttending.text = stats.totalAttending.toString()

        // Update progress bars
        val total = stats.totalGuests.toFloat()
        if (total > 0) {
            val confirmedPercentage = (stats.confirmedGuests / total * 100).toInt()
            val pendingPercentage = (stats.pendingGuests / total * 100).toInt()

            binding.progressConfirmed?.progress = confirmedPercentage
            binding.progressPending?.progress = pendingPercentage

            binding.tvConfirmedPercentage?.text = "$confirmedPercentage%"
            binding.tvPendingPercentage?.text = "$pendingPercentage%"
        }

        // Update guest category breakdown
        updateGuestCategoryBreakdown(stats.categoryBreakdown)

        Log.d(TAG, "Guest stats updated: ${stats.totalGuests} total guests")
    }

    private fun updateGuestCategoryBreakdown(categoryBreakdown: Map<String, Int>) {
        val familyCount = categoryBreakdown["family"] ?: 0
        val friendsCount = categoryBreakdown["friends"] ?: 0
        val colleaguesCount = categoryBreakdown["colleagues"] ?: 0
        val othersCount = categoryBreakdown["others"] ?: 0

        binding.tvFamilyCount?.text = familyCount.toString()
        binding.tvFriendsCount?.text = friendsCount.toString()
        binding.tvColleaguesCount?.text = colleaguesCount.toString()
        binding.tvOthersCount?.text = othersCount.toString()
    }

    private suspend fun loadEventStats() {
        try {
            val result = eventRepository.getAllEvents()
            if (result.isSuccess) {
                val events = result.getOrNull() ?: emptyList()
                updateEventStatsUI(events)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading event stats", e)
        }
    }

    private fun updateEventStatsUI(events: List<Event>) {
        val totalEvents = events.size
        val upcomingEvents = events.count { it.isUpcoming() }
        val completedEvents = events.count { it.isPast() }
        val todayEvents = events.count { it.isToday() }

        binding.tvTotalEvents?.text = totalEvents.toString()
        binding.tvUpcomingEvents?.text = upcomingEvents.toString()
        binding.tvCompletedEvents?.text = completedEvents.toString()
        binding.tvTodayEvents?.text = todayEvents.toString()

        // Show next upcoming event
        val nextEvent = events.filter { it.isUpcoming() }.minByOrNull { it.date }
        if (nextEvent != null) {
            binding.layoutNextEvent?.visibility = View.VISIBLE
            binding.tvNextEventName?.text = nextEvent.name

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvNextEventDate?.text = dateFormat.format(Date(nextEvent.date))

            val daysUntil = nextEvent.getDaysUntilEvent()
            binding.tvNextEventCountdown?.text = when {
                daysUntil == 0L -> "Today"
                daysUntil == 1L -> "Tomorrow"
                daysUntil > 0 -> "$daysUntil days to go"
                else -> "Past event"
            }
        } else {
            binding.layoutNextEvent?.visibility = View.GONE
        }

        Log.d(TAG, "Event stats updated: $totalEvents total events")
    }

    private suspend fun loadVendorStats() {
        try {
            val result = vendorRepository.getAllVendors()
            if (result.isSuccess) {
                val vendors = result.getOrNull() ?: emptyList()
                updateVendorStatsUI(vendors)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vendor stats", e)
        }
    }

    private fun updateVendorStatsUI(vendors: List<Vendor>) {
        val totalVendors = vendors.size
        val bookedVendors = vendors.count { it.isBooked }
        val favoriteVendors = vendors.count { it.isFavorite }
        val confirmedVendors = vendors.count { it.isBooked && it.contractSigned }

        binding.tvTotalVendors?.text = totalVendors.toString()
        binding.tvBookedVendors?.text = bookedVendors.toString()
        binding.tvFavoriteVendors?.text = favoriteVendors.toString()
        binding.tvConfirmedVendors?.text = confirmedVendors.toString()

        // Vendor category breakdown
        val categoryBreakdown = vendors.groupBy { it.category }.mapValues { it.value.size }
        binding.tvPhotographerCount?.text = (categoryBreakdown["photographer"] ?: 0).toString()
        binding.tvCatererCount?.text = (categoryBreakdown["caterer"] ?: 0).toString()
        binding.tvDecoratorCount?.text = (categoryBreakdown["decorator"] ?: 0).toString()

        Log.d(TAG, "Vendor stats updated: $totalVendors total vendors")
    }

    private suspend fun loadWeddingCountdown() {
        try {
            // Load wedding date from preferences or Firestore
            val weddingDate = getWeddingDateFromPreferences()
            if (weddingDate > 0) {
                updateWeddingCountdownUI(weddingDate)
            } else {
                binding.layoutWeddingCountdown?.visibility = View.GONE
                binding.tvSetWeddingDate?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wedding countdown", e)
        }
    }

    private fun updateWeddingCountdownUI(weddingDate: Long) {
        val currentTime = System.currentTimeMillis()
        val timeDifference = weddingDate - currentTime

        if (timeDifference > 0) {
            val days = timeDifference / (1000 * 60 * 60 * 24)
            val hours = (timeDifference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
            val minutes = (timeDifference % (1000 * 60 * 60)) / (1000 * 60)

            binding.layoutWeddingCountdown?.visibility = View.VISIBLE
            binding.tvSetWeddingDate?.visibility = View.GONE

            binding.tvCountdownDays?.text = days.toString()
            binding.tvCountdownHours?.text = hours.toString()
            binding.tvCountdownMinutes?.text = minutes.toString()

            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvWeddingDate?.text = dateFormat.format(Date(weddingDate))

            Log.d(TAG, "Wedding countdown updated: $days days remaining")
        } else {
            // Wedding has passed
            binding.layoutWeddingCountdown?.visibility = View.VISIBLE
            binding.tvCountdownDays?.text = "🎉"
            binding.tvCountdownHours?.text = "Wedding"
            binding.tvCountdownMinutes?.text = "Complete!"
        }
    }

    private suspend fun loadRecentActivity() {
        try {
            // Load recent guests, events, and vendor activities
            loadRecentGuests()
            loadRecentEvents()
            loadRecentVendors()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent activity", e)
        }
    }

    private suspend fun loadRecentGuests() {
        try {
            val result = guestRepository.getAllGuests()
            if (result.isSuccess) {
                val guests = result.getOrNull() ?: emptyList()
                val recentGuests = guests.sortedByDescending { it.createdAt }.take(3)
                updateRecentGuestsUI(recentGuests)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent guests", e)
        }
    }

    private fun updateRecentGuestsUI(recentGuests: List<Guest>) {
        binding.layoutRecentGuests?.removeAllViews()

        if (recentGuests.isEmpty()) {
            binding.tvNoRecentGuests?.visibility = View.VISIBLE
            return
        }

        binding.tvNoRecentGuests?.visibility = View.GONE

        recentGuests.forEach { guest ->
            val guestView = layoutInflater.inflate(R.layout.item_recent_guest, binding.layoutRecentGuests, false)

            // Populate guest view
            val tvGuestName = guestView.findViewById<android.widget.TextView>(R.id.tvRecentGuestName)
            val tvGuestCategory = guestView.findViewById<android.widget.TextView>(R.id.tvRecentGuestCategory)
            val tvGuestStatus = guestView.findViewById<android.widget.TextView>(R.id.tvRecentGuestStatus)

            tvGuestName.text = guest.name
            tvGuestCategory.text = guest.getCategoryDisplayName()
            tvGuestStatus.text = guest.rsvpStatus.replaceFirstChar { it.uppercase() }

            guestView.setOnClickListener {
                val intent = Intent(this, GuestDetailActivity::class.java)
                intent.putExtra("guestId", guest.id)
                startActivity(intent)
            }

            binding.layoutRecentGuests?.addView(guestView)
        }
    }

    private suspend fun loadRecentEvents() {
        try {
            val result = eventRepository.getAllEvents()
            if (result.isSuccess) {
                val events = result.getOrNull() ?: emptyList()
                val recentEvents = events.sortedByDescending { it.createdAt }.take(3)
                updateRecentEventsUI(recentEvents)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent events", e)
        }
    }

    private fun updateRecentEventsUI(recentEvents: List<Event>) {
        binding.layoutRecentEvents?.removeAllViews()

        if (recentEvents.isEmpty()) {
            binding.tvNoRecentEvents?.visibility = View.VISIBLE
            return
        }

        binding.tvNoRecentEvents?.visibility = View.GONE

        recentEvents.forEach { event ->
            val eventView = layoutInflater.inflate(R.layout.item_recent_event, binding.layoutRecentEvents, false)

            // Populate event view
            val tvEventName = eventView.findViewById<android.widget.TextView>(R.id.tvRecentEventName)
            val tvEventType = eventView.findViewById<android.widget.TextView>(R.id.tvRecentEventType)
            val tvEventDate = eventView.findViewById<android.widget.TextView>(R.id.tvRecentEventDate)

            tvEventName.text = event.name
            tvEventType.text = event.getEventTypeDisplayName()

            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            tvEventDate.text = dateFormat.format(Date(event.date))

            eventView.setOnClickListener {
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            }

            binding.layoutRecentEvents?.addView(eventView)
        }
    }

    private suspend fun loadRecentVendors() {
        try {
            val result = vendorRepository.getAllVendors()
            if (result.isSuccess) {
                val vendors = result.getOrNull() ?: emptyList()
                val recentVendors = vendors.sortedByDescending { it.addedAt }.take(3)
                updateRecentVendorsUI(recentVendors)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent vendors", e)
        }
    }

    private fun updateRecentVendorsUI(recentVendors: List<Vendor>) {
        binding.layoutRecentVendors?.removeAllViews()

        if (recentVendors.isEmpty()) {
            binding.tvNoRecentVendors?.visibility = View.VISIBLE
            return
        }

        binding.tvNoRecentVendors?.visibility = View.GONE

        recentVendors.forEach { vendor ->
            val vendorView = layoutInflater.inflate(R.layout.item_recent_vendor, binding.layoutRecentVendors, false)

            // Populate vendor view
            val tvVendorName = vendorView.findViewById<android.widget.TextView>(R.id.tvRecentVendorName)
            val tvVendorCategory = vendorView.findViewById<android.widget.TextView>(R.id.tvRecentVendorCategory)
            val tvVendorStatus = vendorView.findViewById<android.widget.TextView>(R.id.tvRecentVendorStatus)

            tvVendorName.text = vendor.getDisplayName()
            tvVendorCategory.text = vendor.getCategoryDisplayName()
            tvVendorStatus.text = vendor.getStatusDisplayName()

            vendorView.setOnClickListener {
                val intent = Intent(this, VendorDetailActivity::class.java)
                intent.putExtra("vendorId", vendor.id)
                startActivity(intent)
            }

            binding.layoutRecentVendors?.addView(vendorView)
        }
    }

    private fun getWeddingDateFromPreferences(): Long {
        val sharedPrefs = getSharedPreferences("wedding_prefs", MODE_PRIVATE)
        return sharedPrefs.getLong("wedding_date", 0L)
    }

    private fun saveWeddingDateToPreferences(date: Long) {
        val sharedPrefs = getSharedPreferences("wedding_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putLong("wedding_date", date).apply()
    }

    private fun showSetWeddingDateDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Wedding Date")
            .setMessage("Set your wedding date to see countdown and better planning")
            .setPositiveButton("Set Date") { _, _ ->
                showDatePicker()
            }
            .setNegativeButton("Later", null)
            .create()

        dialog.show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 12, 0, 0) // Set to noon

                val selectedDate = selectedCalendar.timeInMillis
                saveWeddingDateToPreferences(selectedDate)
                updateWeddingCountdownUI(selectedDate)

                Toast.makeText(this, "Wedding date set successfully!", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showUserProfileDialog() {
        val currentUser = auth.currentUser
        val message = """
            Name: ${currentUser?.displayName ?: "Not set"}
            Email: ${currentUser?.email ?: "Not set"}
            User ID: ${currentUser?.uid ?: "Not available"}
            Account Created: ${currentUser?.metadata?.creationTimestamp?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("User Profile")
            .setMessage(message)
            .setPositiveButton("Edit Profile") { _, _ ->
                // TODO: Implement profile editing
                Toast.makeText(this, "Profile editing - Coming soon!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Logout") { _, _ ->
                showLogoutConfirmation()
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        // TODO: Replace with your actual login activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "Notification Settings",
            "Data Export",
            "Privacy Settings",
            "App Information",
            "Help & Support"
        )

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showNotificationSettings()
                    1 -> showDataExportOptions()
                    2 -> showPrivacySettings()
                    3 -> showAppInformation()
                    4 -> showHelpAndSupport()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showNotificationSettings() {
        Toast.makeText(this, "Notification settings - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showDataExportOptions() {
        showExportOptions()
    }

    private fun showPrivacySettings() {
        Toast.makeText(this, "Privacy settings - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showAppInformation() {
        val appInfo = """
            ShaadiSaathi Wedding App
            Version: 1.0.0 (Phase 4)
            
            Features:
            • Guest Management
            • Event Timeline
            • Vendor Directory
            • Photo Gallery
            • Chat Rooms
            
            Developed with ❤️ for your special day
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("App Information")
            .setMessage(appInfo)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpAndSupport() {
        val helpInfo = """
            Need help? Here are your options:
            
            • Check the in-app tutorials
            • Visit our FAQ section
            • Contact support team
            • Join our community forum
            
            Email: support@shaadisaathi.com
            Phone: +91 12345 67890
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage(helpInfo)
            .setPositiveButton("Contact Support") { _, _ ->
                // TODO: Implement support contact
                Toast.makeText(this, "Opening support - Coming soon!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showExportOptions() {
        val options = arrayOf(
            "Export Guest List (CSV)",
            "Export Event Timeline (CSV)",
            "Export Vendor List (CSV)",
            "Export All Data (ZIP)"
        )

        AlertDialog.Builder(this)
            .setTitle("Export Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportGuestList()
                    1 -> exportEventTimeline()
                    2 -> exportVendorList()
                    3 -> exportAllData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportGuestList() {
        val intent = Intent(this, GuestListActivity::class.java)
        intent.putExtra("auto_export", true)
        startActivity(intent)
    }

    private fun exportEventTimeline() {
        Toast.makeText(this, "Event export - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun exportVendorList() {
        Toast.makeText(this, "Vendor export - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun exportAllData() {
        Toast.makeText(this, "Full data export - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showFeatureComingSoon(featureName: String) {
        AlertDialog.Builder(this)
            .setTitle("Coming Soon!")
            .setMessage("$featureName will be available in the next update.\n\nStay tuned for more amazing features!")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                showNotifications()
                true
            }
            R.id.action_search -> {
                showGlobalSearch()
                true
            }
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            R.id.action_help -> {
                showHelpAndSupport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNotifications() {
        Toast.makeText(this, "Notifications - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showGlobalSearch() {
        Toast.makeText(this, "Global search - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning to main activity
        loadDashboardData()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}
