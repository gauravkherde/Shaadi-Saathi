package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityGuestMainBinding

class GuestMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestMainBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "GuestMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        loadUserName()
        loadGuestStats()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Phase 2 Features
        binding.btnViewEvents.setOnClickListener {
            startActivity(Intent(this, EventTimelineActivity::class.java))
        }

        binding.btnRSVP.setOnClickListener {
            startActivity(Intent(this, RSVPActivity::class.java))
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
                val intent = Intent(this, PhotoGalleryActivity::class.java)
                intent.putExtra("albumId", "default_album")
                intent.putExtra("albumName", "Wedding Photos")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening PhotoGalleryActivity", e)
                Toast.makeText(this, "Error opening Photo Gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogoutGuest.setOnClickListener {
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

                        Log.d(TAG, "User name: $name, role: $role")

                        binding.tvGuestWelcome.text = "Welcome, $name!"
                        binding.tvGuestSubtitle.text = "Join the wedding celebration"

                        // Verify user is actually a guest
                        if (role != "guest") {
                            Log.w(TAG, "User role mismatch. Expected 'guest', got '$role'")
                            Toast.makeText(this, "Access denied. Please login with correct role.", Toast.LENGTH_LONG).show()
                            performLogout()
                        }
                    } else {
                        Log.w(TAG, "User document does not exist")
                        binding.tvGuestWelcome.text = "Welcome Guest!"
                        binding.tvGuestSubtitle.text = "Join the wedding celebration"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data", e)
                    binding.tvGuestWelcome.text = "Welcome Guest!"
                    binding.tvGuestSubtitle.text = "Join the wedding celebration"
                }
        } else {
            Log.w(TAG, "No current user found")
            // Redirect to login if no user
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadGuestStats() {
        val currentUser = auth.currentUser ?: return

        // Load total events count
        firestore.collection("events")
            .get()
            .addOnSuccessListener { events ->
                val eventCount = events.size()
                Log.d(TAG, "Total events available: $eventCount")
                // You can update UI with this info if needed
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading event stats", e)
            }

        // Load user's RSVP count
        firestore.collection("rsvps")
            .whereEqualTo("guestId", currentUser.uid)
            .get()
            .addOnSuccessListener { rsvps ->
                val rsvpCount = rsvps.size()
                Log.d(TAG, "User has $rsvpCount RSVPs")
                // You can update UI with this info if needed
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading RSVP stats", e)
            }
    }

    private fun performLogout() {
        Log.d(TAG, "Performing logout")

        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to this activity
        loadUserName()
        loadGuestStats()
    }

    override fun onBackPressed() {
        // Prevent accidental exits, show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
