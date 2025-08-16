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
import com.gaurav.shaadisaathi.activities.AddVendorActivity
import com.gaurav.shaadisaathi.activities.VendorDetailActivity
import com.gaurav.shaadisaathi.adapters.VendorListAdapter
import com.gaurav.shaadisaathi.databinding.FragmentVendorListBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.repository.VendorRepository
import kotlinx.coroutines.launch

class VendorListFragment : Fragment() {

    private var _binding: FragmentVendorListBinding? = null
    private val binding get() = _binding!!

    private val vendorRepository = VendorRepository()
    private lateinit var vendorAdapter: VendorListAdapter
    private val vendorList = mutableListOf<Vendor>()
    private val TAG = "VendorListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVendorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadVendors()
    }

    private fun setupRecyclerView() {
        vendorAdapter = VendorListAdapter(
            vendors = vendorList,
            onVendorClick = { vendor ->
                openVendorDetail(vendor)
            },
            onFavoriteClick = { vendor ->
                toggleFavorite(vendor)
            },
            onBookClick = { vendor ->
                bookVendor(vendor)
            },
            onCallClick = { vendor ->
                callVendor(vendor)
            }
        )

        binding.recyclerViewVendors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vendorAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddVendor?.setOnClickListener {
            val intent = Intent(requireContext(), AddVendorActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadVendors()
        }
    }

    private fun loadVendors() {
        binding.progressBar?.visibility = View.VISIBLE
        binding.swipeRefreshLayout?.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = vendorRepository.getAllVendors()
                if (result.isSuccess) {
                    val vendors = result.getOrNull() ?: emptyList()
                    vendorList.clear()
                    vendorList.addAll(vendors.sortedBy { it.name })
                    vendorAdapter.notifyDataSetChanged()
                    updateEmptyState(vendors.isEmpty())
                } else {
                    Log.e(TAG, "Error loading vendors: ${result.exceptionOrNull()}")
                    Toast.makeText(requireContext(), "Error loading vendors", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading vendors", e)
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
            binding.recyclerViewVendors.visibility = View.GONE
        } else {
            binding.layoutEmptyState?.visibility = View.GONE
            binding.recyclerViewVendors.visibility = View.VISIBLE
        }
    }

    private fun openVendorDetail(vendor: Vendor) {
        val intent = Intent(requireContext(), VendorDetailActivity::class.java)
        intent.putExtra("vendorId", vendor.id)
        startActivity(intent)
    }

    private fun toggleFavorite(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                val updatedVendor = vendor.copy(
                    isFavorite = !vendor.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    val message = if (updatedVendor.isFavorite) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    loadVendors()
                } else {
                    Toast.makeText(requireContext(), "Error updating vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bookVendor(vendor: Vendor) {
        if (vendor.isBooked) {
            Toast.makeText(requireContext(), "Vendor is already booked", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Book Vendor")
            .setMessage("Book ${vendor.getDisplayName()} for your wedding?")
            .setPositiveButton("Book") { _, _ ->
                performBookVendor(vendor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBookVendor(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                val updatedVendor = vendor.copy(
                    isBooked = true,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "${vendor.getDisplayName()} booked successfully!", Toast.LENGTH_SHORT).show()
                    loadVendors()
                } else {
                    Toast.makeText(requireContext(), "Error booking vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error booking vendor", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun callVendor(vendor: Vendor) {
        val phoneNumber = vendor.contactInfo.primaryPhone
        if (phoneNumber.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to make call", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadVendors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = VendorListFragment()
    }
}
