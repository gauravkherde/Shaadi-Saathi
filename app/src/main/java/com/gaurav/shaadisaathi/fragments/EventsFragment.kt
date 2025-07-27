package com.gaurav.shaadisaathi.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.activities.AddEventActivity
import com.gaurav.shaadisaathi.activities.EventDetailActivity
import com.gaurav.shaadisaathi.adapters.EventTimelineAdapter
import com.gaurav.shaadisaathi.databinding.FragmentEventsBinding
import com.gaurav.shaadisaathi.models.Event
import com.gaurav.shaadisaathi.repository.EventRepository
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val eventRepository = EventRepository()
    private lateinit var eventAdapter: EventTimelineAdapter
    private val eventList = mutableListOf<Event>()
    private val TAG = "EventsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadEvents()
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
                deleteEvent(event)
            }
        )

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEvent.setOnClickListener {
            val intent = Intent(requireContext(), AddEventActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadEvents()
        }
    }

    private fun loadEvents() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllEvents()
                if (result.isSuccess) {
                    val events = result.getOrNull() ?: emptyList()
                    eventList.clear()
                    eventList.addAll(events.sortedBy { it.date })
                    eventAdapter.notifyDataSetChanged()
                    updateEmptyState(events.isEmpty())
                } else {
                    Log.e(TAG, "Error loading events: ${result.exceptionOrNull()}")
                    Toast.makeText(requireContext(), "Error loading events", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading events", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewEvents.visibility = View.VISIBLE
        }
    }

    private fun openEventDetail(event: Event) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java)
        intent.putExtra("eventId", event.id)
        startActivity(intent)
    }

    private fun editEvent(event: Event) {
        val intent = Intent(requireContext(), com.gaurav.shaadisaathi.activities.EditEventActivity::class.java)
        intent.putExtra("eventId", event.id)
        startActivity(intent)
    }

    private fun deleteEvent(event: Event) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete '${event.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteEvent(event: Event) {
        lifecycleScope.launch {
            try {
                val result = eventRepository.deleteEvent(event.id)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show()
                    loadEvents()
                } else {
                    Toast.makeText(requireContext(), "Error deleting event", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting event", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EventsFragment()
    }
}
