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
import com.gaurav.shaadisaathi.activities.AddGuestActivity
import com.gaurav.shaadisaathi.activities.GuestDetailActivity
import com.gaurav.shaadisaathi.adapters.GuestListAdapter
import com.gaurav.shaadisaathi.databinding.FragmentGuestListBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.GuestRepository
import kotlinx.coroutines.launch

class GuestListFragment : Fragment() {

    private var _binding: FragmentGuestListBinding? = null
    private val binding get() = _binding!!

    private val guestRepository = GuestRepository()
    private lateinit var guestAdapter: GuestListAdapter
    private val guestList = mutableListOf<Guest>()
    private val TAG = "GuestListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadGuests()
    }

    private fun setupRecyclerView() {
        guestAdapter = GuestListAdapter(
            guests = guestList,
            onGuestClick = { guest ->
                openGuestDetail(guest)
            },
            onCallClick = { guest ->
                callGuest(guest)
            },
            onEmailClick = { guest ->
                emailGuest(guest)
            }
        )

        binding.recyclerViewGuests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guestAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddGuest?.setOnClickListener {
            val intent = Intent(requireContext(), AddGuestActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadGuests()
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
                    guestAdapter.notifyDataSetChanged()
                    updateEmptyState(guests.isEmpty())
                    updateGuestStatistics(guests)
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

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState?.visibility = View.VISIBLE
            binding.recyclerViewGuests.visibility = View.GONE
            binding.layoutGuestStats?.visibility = View.GONE
        } else {
            binding.layoutEmptyState?.visibility = View.GONE
            binding.recyclerViewGuests.visibility = View.VISIBLE
            binding.layoutGuestStats?.visibility = View.VISIBLE
        }
    }

    private fun updateGuestStatistics(guests: List<Guest>) {
        val totalGuests = guests.size
        val confirmedGuests = guests.count { it.rsvpStatus == "confirmed" }
        val pendingGuests = guests.count { it.rsvpStatus == "pending" }
        val declinedGuests = guests.count { it.rsvpStatus == "declined" }
        val totalAttending = guests.sumOf { if (it.rsvpStatus == "confirmed") it.getTotalGuests() else 0 }

        binding.tvTotalGuests?.text = totalGuests.toString()
        binding.tvConfirmedGuests?.text = confirmedGuests.toString()
        binding.tvPendingGuests?.text = pendingGuests.toString()
        binding.tvDeclinedGuests?.text = declinedGuests.toString()
        binding.tvTotalAttending?.text = totalAttending.toString()

        // Update percentages
        val total = totalGuests.toFloat()
        if (total > 0) {
            val confirmedPct = (confirmedGuests / total * 100).toInt()
            val pendingPct = (pendingGuests / total * 100).toInt()
            val declinedPct = (declinedGuests / total * 100).toInt()

            binding.tvConfirmedPercentage?.text = "$confirmedPct%"
            binding.tvPendingPercentage?.text = "$pendingPct%"
            binding.tvDeclinedPercentage?.text = "$declinedPct%"
        }
    }

    private fun openGuestDetail(guest: Guest) {
        val intent = Intent(requireContext(), GuestDetailActivity::class.java)
        intent.putExtra("guestId", guest.id)
        startActivity(intent)
    }

    private fun callGuest(guest: Guest) {
        if (guest.phone.isEmpty()) {
            Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:${guest.phone}")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            Toast.makeText(requireContext(), "Unable to make call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun emailGuest(guest: Guest) {
        if (guest.email.isEmpty()) {
            Toast.makeText(requireContext(), "No email address available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = android.net.Uri.parse("mailto:${guest.email}")
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wedding Invitation - ${guest.name}")
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email", e)
            Toast.makeText(requireContext(), "Unable to send email", Toast.LENGTH_SHORT).show()
        }
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
        fun newInstance() = GuestListFragment()
    }
}
