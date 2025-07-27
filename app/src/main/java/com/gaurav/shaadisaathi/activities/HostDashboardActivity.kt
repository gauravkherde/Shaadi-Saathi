package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityHostDashboardBinding

class HostDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostDashboardBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "HostDashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Creating HostDashboardActivity")

            binding = ActivityHostDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = FirebaseAuth.getInstance()

            Log.d(TAG, "HostDashboardActivity created successfully")

            setupClickListeners()
            loadUserName()
            loadDashboardStats()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error creating dashboard: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        // Phase 2 Features
        binding.btnManageGuests.setOnClickListener {
            try {
                Log.d(TAG, "Opening Guest Management")
                startActivity(Intent(this, GuestListActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening GuestListActivity", e)
                Toast.makeText(this, "Error opening Guest Management: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEventTimeline.setOnClickListener {
            try {
                Log.d(TAG, "Opening Event Timeline")
                startActivity(Intent(this, EventTimelineActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening EventTimelineActivity", e)
                Toast.makeText(this, "Error opening Event Timeline: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddEvent.setOnClickListener {
            try {
                Log.d(TAG, "Opening Add Event")
                startActivity(Intent(this, AddEventActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening AddEventActivity", e)
                Toast.makeText(this, "Error opening Add Event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddGuest.setOnClickListener {
            try {
                Log.d(TAG, "Opening Add Guest")
                startActivity(Intent(this, AddGuestActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening AddGuestActivity", e)
                Toast.makeText(this, "Error opening Add Guest: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewRSVPs.setOnClickListener {
            try {
                Log.d(TAG, "Opening RSVP Management")
                startActivity(Intent(this, RSVPActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening RSVPActivity", e)
                Toast.makeText(this, "Error opening RSVP Management: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Phase 3 Features - NEW ADDITIONS
        binding.btnChatRooms.setOnClickListener {
            try {
                Log.d(TAG, "Opening Chat Rooms")
                startActivity(Intent(this, ChatListActivity::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error opening ChatListActivity", e)
                Toast.makeText(this, "Error opening Chat Rooms: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPhotoGallery.setOnClickListener {
            try {
                Log.d(TAG, "Opening Photo Gallery")
                // You can pass a default album ID or create a general album
                val intent = Intent(this, PhotoGalleryActivity::class.java)
                intent.putExtra("albumId", "default_album")
                intent.putExtra("albumName", "Wedding Photos")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening PhotoGalleryActivity", e)
                Toast.makeText(this, "Error opening Photo Gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Statistics Card Click Listeners
        binding.cardGuestStats.setOnClickListener {
            try {
                startActivity(Intent(this, GuestListActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Opening guest list...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardEventStats.setOnClickListener {
            try {
                startActivity(Intent(this, EventTimelineActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Opening event timeline...", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout functionality
        binding.btnLogoutHost.setOnClickListener {
            performLogout()
        }
    }

    private fun loadUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Loading user data for: ${currentUser.uid}")

            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name")
                        val role = doc.getString("role")
                        val email = doc.getString("email")

                        Log.d(TAG, "User name: $name, role: $role, email: $email")

                        binding.tvHostWelcome.text = "Welcome, $name!"
                        binding.tvHostSubtitle.text = "Manage your wedding events & guests"

                        // Verify user is actually a host
                        if (role != "host") {
                            Log.w(TAG, "User role mismatch. Expected 'host', got '$role'")
                            Toast.makeText(this, "Access denied. Please login as host.", Toast.LENGTH_LONG).show()
                            performLogout()
                        }
                    } else {
                        Log.w(TAG, "User document does not exist")
                        binding.tvHostWelcome.text = "Welcome Host!"
                        binding.tvHostSubtitle.text = "Manage your wedding events & guests"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data", e)
                    binding.tvHostWelcome.text = "Welcome Host!"
                    binding.tvHostSubtitle.text = "Manage your wedding events & guests"
                }
        } else {
            Log.w(TAG, "No current user found")
            // Redirect to login if no user
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadDashboardStats() {
        val currentUser = auth.currentUser ?: return

        Log.d(TAG, "Loading dashboard statistics for host: ${currentUser.uid}")

        // Load guest count
        firestore.collection("guests")
            .whereEqualTo("hostId", currentUser.uid)
            .get()
            .addOnSuccessListener { guests ->
                val guestCount = guests.size()
                binding.tvGuestCount.text = "$guestCount"
                binding.tvGuestLabel.text = if (guestCount == 1) "Guest Invited" else "Guests Invited"
                Log.d(TAG, "Loaded $guestCount guests")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading guest count", e)
                binding.tvGuestCount.text = "0"
                binding.tvGuestLabel.text = "Guests Invited"
            }

        // Load event count
        firestore.collection("events")
            .whereEqualTo("hostId", currentUser.uid)
            .get()
            .addOnSuccessListener { events ->
                val eventCount = events.size()
                binding.tvEventCount.text = "$eventCount"
                binding.tvEventLabel.text = if (eventCount == 1) "Event Planned" else "Events Planned"
                Log.d(TAG, "Loaded $eventCount events")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading event count", e)
                binding.tvEventCount.text = "0"
                binding.tvEventLabel.text = "Events Planned"
            }

        // Load RSVP statistics
        firestore.collection("rsvps")
            .get()
            .addOnSuccessListener { rsvps ->
                var acceptedCount = 0
                var pendingCount = 0

                rsvps.documents.forEach { doc ->
                    val status = doc.getString("status")
                    when (status) {
                        "accepted" -> acceptedCount++
                        "pending" -> pendingCount++
                    }
                }

                binding.tvRSVPCount.text = "$acceptedCount"
                binding.tvRSVPLabel.text = if (acceptedCount == 1) "RSVP Confirmed" else "RSVPs Confirmed"
                Log.d(TAG, "RSVP Stats - Accepted: $acceptedCount, Pending: $pendingCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading RSVP stats", e)
                binding.tvRSVPCount.text = "0"
                binding.tvRSVPLabel.text = "RSVPs Confirmed"
            }
    }

    private fun performLogout() {
        try {
            Log.d(TAG, "Performing logout")

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard stats when returning to this activity
        loadDashboardStats()
        Log.d(TAG, "HostDashboardActivity onResume - refreshing stats")
    }

    override fun onBackPressed() {
        // Prevent accidental exits, show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit ShaadiSaathi?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
                finishAffinity() // Close all activities and exit app
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HostDashboardActivity onDestroy")
    }
}
