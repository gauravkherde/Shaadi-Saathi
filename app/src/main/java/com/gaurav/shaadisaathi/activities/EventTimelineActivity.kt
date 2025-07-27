package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.gaurav.shaadisaathi.adapters.EventAdapter
import com.gaurav.shaadisaathi.databinding.ActivityEventTimelineBinding
import com.gaurav.shaadisaathi.models.Event

class EventTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventTimelineBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()
    private val TAG = "EventTimelineActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadEvents()

        binding.fabAddEvent.setOnClickListener {
            // Check if user is host before allowing event creation
            checkUserRoleAndNavigate()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(eventList) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@EventTimelineActivity)
            adapter = eventAdapter
        }
    }

    private fun loadEvents() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        // Check user role first
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                val userRole = userDoc.getString("role")
                Log.d(TAG, "User role: $userRole")

                if (userRole == "host") {
                    // Load events created by this host
                    loadHostEvents(currentUser.uid)
                    // Show FAB for hosts
                    binding.fabAddEvent.visibility = View.VISIBLE
                } else {
                    // Load all events for guests (they can see all events)
                    loadAllEvents()
                    // Hide FAB for guests
                    binding.fabAddEvent.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user role", e)
                binding.progressBar.visibility = View.GONE
                loadAllEvents() // Fallback to show all events
            }
    }

    private fun loadHostEvents(hostId: String) {
        Log.d(TAG, "Loading events for host: $hostId")

        firestore.collection("events")
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshots, e ->
                handleEventsSnapshot(snapshots, e)
            }
    }

    private fun loadAllEvents() {
        Log.d(TAG, "Loading all events for guest view")

        firestore.collection("events")
            .addSnapshotListener { snapshots, e ->
                handleEventsSnapshot(snapshots, e)
            }
    }

    private fun handleEventsSnapshot(snapshots: QuerySnapshot?, e: Exception?) {
        binding.progressBar.visibility = View.GONE

        if (e != null) {
            Log.e(TAG, "Error loading events", e)
            return
        }

        eventList.clear()
        snapshots?.documents?.forEach { doc ->
            val event = doc.toObject(Event::class.java)
            event?.let {
                Log.d(TAG, "Loaded event: ${it.name}")
                eventList.add(it)
            }
        }

        // Sort events by date (you might want to add proper date parsing here)
        eventList.sortBy { it.date }

        eventAdapter.notifyDataSetChanged()

        if (eventList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewEvents.visibility = View.VISIBLE
        }

        Log.d(TAG, "Total events loaded: ${eventList.size}")
    }

    private fun checkUserRoleAndNavigate() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                val userRole = userDoc.getString("role")
                if (userRole == "host") {
                    startActivity(Intent(this, AddEventActivity::class.java))
                } else {
                    // Should not happen as FAB is hidden for guests, but just in case
                    android.widget.Toast.makeText(this, "Only hosts can create events", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        // Refresh events when returning from AddEventActivity
        loadEvents()
    }
}
