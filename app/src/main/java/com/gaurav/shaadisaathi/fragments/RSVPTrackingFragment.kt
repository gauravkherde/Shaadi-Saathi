package com.gaurav.shaadisaathi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.RSVPAdapter
import com.gaurav.shaadisaathi.databinding.FragmentRsvpTrackingBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.GuestRepository
import kotlinx.coroutines.launch

class RSVPTrackingFragment : Fragment() {

    private var _binding: FragmentRsvpTrackingBinding? = null
    private val binding get() = _binding!!

    private val guestRepository = GuestRepository()
    private lateinit var rsvpAdapter: RSVPAdapter
    private val guestList = mutableListOf<Guest>()
    private val TAG = "RSVPTrackingFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRsvpTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadGuests()
    }

    private fun setupRecyclerView() {
        rsvpAdapter = RSVPAdapter(
            guests = guestList,
            onStatusUpdate = { guest, newStatus ->
                updateRSVPStatus(guest, newStatus)
            },
            onSendReminder = { guest ->
                sendRSVPReminder(guest)
            }
        )

        binding.recyclerViewRsvp.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rsvpAdapter
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadGuests()
        }

        binding.chipFilterAll?.setOnClickListener {
            filterGuests("all")
        }

        binding.chipFilterPending?.setOnClickListener {
            filterGuests("pending")
        }

        binding.chipFilterConfirmed?.setOnClickListener {
            filterGuests("confirmed")
        }

        binding.chipFilterDeclined?.setOnClickListener {
            filterGuests("declined")
        }
    }

    private fun loadGuests() {
        binding.progressBar?.visibility = View.VISIBLE
        binding.swipeRefreshLayout?.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = guestRepository.getAllGuests()
                if (result.isSuccess) {
                    val guests = result.getOrNull() ?: emptyList()
                    guestList.clear()
                    guestList.addAll(guests.sortedBy { it.name })
                    rsvpAdapter.notifyDataSetChanged()
                    updateEmptyState(guests.isEmpty())
                    updateRSVPStatistics(guests)
                } else {
                    Log.e(TAG, "Error loading guests: ${result.exceptionOrNull()}")
                    Toast.makeText(requireContext(), "Error loading guests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading guests", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
                binding.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun filterGuests(status: String) {
        val filteredGuests = when (status) {
            "all" -> guestList
            else -> guestList.filter { it.rsvpStatus == status }
        }

        rsvpAdapter.updateGuests(filteredGuests)
        updateFilterChips(status)
    }

    private fun updateFilterChips(selectedStatus: String) {
        binding.chipFilterAll?.isChecked = selectedStatus == "all"
        binding.chipFilterPending?.isChecked = selectedStatus == "pending"
        binding.chipFilterConfirmed?.isChecked = selectedStatus == "confirmed"
        binding.chipFilterDeclined?.isChecked = selectedStatus == "declined"
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState?.visibility = View.VISIBLE
            binding.recyclerViewRsvp.visibility = View.GONE
            binding.layoutRsvpStats?.visibility = View.GONE
        } else {
            binding.layoutEmptyState?.visibility = View.GONE
            binding.recyclerViewRsvp.visibility = View.VISIBLE
            binding.layoutRsvpStats?.visibility = View.VISIBLE
        }
    }

    private fun updateRSVPStatistics(guests: List<Guest>) {
        val totalGuests = guests.size
        val confirmedGuests = guests.count { it.rsvpStatus == "confirmed" }
        val pendingGuests = guests.count { it.rsvpStatus == "pending" }
        val declinedGuests = guests.count { it.rsvpStatus == "declined" }
        val responseRate = if (totalGuests > 0) ((totalGuests - pendingGuests) / totalGuests.toFloat() * 100).toInt() else 0

        binding.tvTotalInvited?.text = totalGuests.toString()
        binding.tvConfirmedCount?.text = confirmedGuests.toString()
        binding.tvPendingCount?.text = pendingGuests.toString()
        binding.tvDeclinedCount?.text = declinedGuests.toString()
        binding.tvResponseRate?.text = "$responseRate%"

        // Update progress bar
        binding.progressResponseRate?.progress = responseRate
    }

    private fun updateRSVPStatus(guest: Guest, newStatus: String) {
        lifecycleScope.launch {
            try {
                val updatedGuest = guest.copy(
                    rsvpStatus = newStatus,
                    rsvpResponseAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val result = guestRepository.updateGuest(updatedGuest)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "RSVP updated for ${guest.name}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "RSVP updated: ${guest.id} -> $newStatus")
                    loadGuests() // Refresh display
                } else {
                    Log.e(TAG, "Error updating RSVP", result.exceptionOrNull())
                    Toast.makeText(requireContext(), "Error updating RSVP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating RSVP", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendRSVPReminder(guest: Guest) {
        if (guest.email.isEmpty() && guest.phone.isEmpty()) {
            Toast.makeText(requireContext(), "No contact information available", Toast.LENGTH_SHORT).show()
            return
        }

        // For now, show a confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Send RSVP Reminder")
            .setMessage("Send RSVP reminder to ${guest.name}?")
            .setPositiveButton("Send") { _, _ ->
                // TODO: Implement actual reminder sending
                Toast.makeText(requireContext(), "RSVP reminder sent to ${guest.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadGuests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = RSVPTrackingFragment()
    }
}
