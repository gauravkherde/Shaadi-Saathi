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
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.EventTimelineAdapter
import com.gaurav.shaadisaathi.databinding.ActivityEventTimelineBinding
import com.gaurav.shaadisaathi.models.Event
import com.gaurav.shaadisaathi.repository.EventRepository
import com.gaurav.shaadisaathi.utils.CalendarIntegration
import kotlinx.coroutines.launch

class EventTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventTimelineBinding
    private lateinit var auth: FirebaseAuth
    private val eventRepository = EventRepository()
    private lateinit var eventAdapter: EventTimelineAdapter
    private val eventList = mutableListOf<Event>()
    private val TAG = "EventTimelineActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadEvents()

        Log.d(TAG, "EventTimelineActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Event Timeline"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventTimelineAdapter(
            events = eventList,
            onEventClick = { event ->
                openEventDetail(event)
            },
            onEditClick = { event ->
                editEvent(event)
            },
            onDeleteClick = { event ->
                showDeleteConfirmation(event)
            }
        )

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@EventTimelineActivity)
            adapter = eventAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEvent.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }

        binding.btnAddToCalendar.setOnClickListener {
            addAllEventsToCalendar()
        }
    }

    private fun loadEvents() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllEvents()
                if (result.isSuccess) {
                    val events = result.getOrNull() ?: emptyList()
                    eventList.clear()
                    eventList.addAll(events.sortedBy { it.date })
                    eventAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } else {
                    Log.e(TAG, "Error loading events: ${result.exceptionOrNull()}")
                    Toast.makeText(this@EventTimelineActivity, "Error loading events", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading events", e)
                Toast.makeText(this@EventTimelineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openEventDetail(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java)
        intent.putExtra("eventId", event.id)
        startActivity(intent)
    }

    private fun editEvent(event: Event) {
        val intent = Intent(this, EditEventActivity::class.java)
        intent.putExtra("eventId", event.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete ${event.name}?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        lifecycleScope.launch {
            try {
                val result = eventRepository.deleteEvent(event.id)
                if (result.isSuccess) {
                    Toast.makeText(this@EventTimelineActivity, "${event.name} deleted successfully", Toast.LENGTH_SHORT).show()
                    loadEvents()
                } else {
                    Toast.makeText(this@EventTimelineActivity, "Error deleting event", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting event", e)
                Toast.makeText(this@EventTimelineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addAllEventsToCalendar() {
        val weddingEvents = eventList.map { event ->
            CalendarIntegration.WeddingEvent(
                title = event.name,
                description = event.description,
                location = event.venue.getFullAddress(),
                startTime = event.date,
                endTime = event.date + (4 * 60 * 60 * 1000), // 4 hours default
                allDay = false
            )
        }

        CalendarIntegration.addMultipleWeddingEvents(this, weddingEvents)
        Toast.makeText(this, "Events added to calendar!", Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState() {
        if (eventList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewEvents.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event_timeline, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_calendar_sync -> {
                addAllEventsToCalendar()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }
}
