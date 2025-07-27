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
import com.gaurav.shaadisaathi.adapters.VendorListAdapter
import com.gaurav.shaadisaathi.databinding.ActivityVendorDirectoryBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.repository.VendorRepository
import kotlinx.coroutines.launch

class VendorDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVendorDirectoryBinding
    private lateinit var auth: FirebaseAuth
    private val vendorRepository = VendorRepository()
    private lateinit var vendorAdapter: VendorListAdapter
    private val vendorList = mutableListOf<Vendor>()
    private val originalVendorList = mutableListOf<Vendor>()
    private val TAG = "VendorDirectoryActivity"

    private var currentCategory = "all"
    private var showFavoritesOnly = false
    private var showBookedOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendorDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadVendors()

        Log.d(TAG, "VendorDirectoryActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Vendor Directory"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        vendorAdapter = VendorListAdapter(
            vendors = vendorList,
            onVendorClick = { vendor ->
                openVendorDetail(vendor)
            },
            onFavoriteClick = { vendor ->
                toggleFavoriteStatus(vendor)
            },
            onBookClick = { vendor ->
                bookVendor(vendor)
            },
            onCallClick = { vendor ->
                callVendor(vendor)
            }
        )

        binding.recyclerViewVendors.apply {
            layoutManager = LinearLayoutManager(this@VendorDirectoryActivity)
            adapter = vendorAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddVendor.setOnClickListener {
            val intent = Intent(this, AddVendorActivity::class.java)
            startActivity(intent)
        }

        // Category filters
        binding.chipAllVendors.setOnClickListener { filterByCategory("all") }
        binding.chipPhotographer.setOnClickListener { filterByCategory("photographer") }
        binding.chipCaterer.setOnClickListener { filterByCategory("caterer") }
        binding.chipDecorator.setOnClickListener { filterByCategory("decorator") }
        binding.chipDj.setOnClickListener { filterByCategory("dj") }
        binding.chipMakeup.setOnClickListener { filterByCategory("makeup") }

        // Additional filters
        binding.chipFavorites.setOnCheckedChangeListener { _, isChecked ->
            showFavoritesOnly = isChecked
            applyCurrentFilters()
        }

        binding.chipBooked.setOnCheckedChangeListener { _, isChecked ->
            showBookedOnly = isChecked
            applyCurrentFilters()
        }

        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadVendors()
        }
    }

    private fun loadVendors() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout?.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = vendorRepository.getAllVendors()
                if (result.isSuccess) {
                    val vendors = result.getOrNull() ?: emptyList()
                    originalVendorList.clear()
                    originalVendorList.addAll(vendors.sortedBy { it.name })
                    applyCurrentFilters()
                    updateStats()
                } else {
                    Log.e(TAG, "Error loading vendors: ${result.exceptionOrNull()}")
                    Toast.makeText(this@VendorDirectoryActivity, "Error loading vendors", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading vendors", e)
                Toast.makeText(this@VendorDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun filterByCategory(category: String) {
        currentCategory = category
        updateCategoryChipSelection()
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        var filteredList = originalVendorList.toList()

        // Apply category filter
        if (currentCategory != "all") {
            filteredList = filteredList.filter { vendor ->
                vendor.category == currentCategory
            }
        }

        // Apply favorites filter
        if (showFavoritesOnly) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        // Apply booked filter
        if (showBookedOnly) {
            filteredList = filteredList.filter { it.isBooked }
        }

        // Update the displayed list
        vendorList.clear()
        vendorList.addAll(filteredList)
        vendorAdapter.notifyDataSetChanged()
        updateEmptyState(filteredList.isEmpty())

        Log.d(TAG, "Applied filters - showing ${filteredList.size} out of ${originalVendorList.size} vendors")
    }

    private fun updateCategoryChipSelection() {
        binding.chipAllVendors.isChecked = currentCategory == "all"
        binding.chipPhotographer.isChecked = currentCategory == "photographer"
        binding.chipCaterer.isChecked = currentCategory == "caterer"
        binding.chipDecorator.isChecked = currentCategory == "decorator"
        binding.chipDj.isChecked = currentCategory == "dj"
        binding.chipMakeup.isChecked = currentCategory == "makeup"
    }

    private fun updateStats() {
        val totalVendors = originalVendorList.size
        val bookedVendors = originalVendorList.count { it.isBooked }
        val favoriteVendors = originalVendorList.count { it.isFavorite }

        binding.tvTotalVendors.text = "Total: $totalVendors"
        binding.tvBookedVendors.text = "Booked: $bookedVendors"
        binding.tvFavoriteVendors.text = "Favorites: $favoriteVendors"

        // Update category counts
        val photographerCount = originalVendorList.count { it.category == "photographer" }
        val catererCount = originalVendorList.count { it.category == "caterer" }
        val decoratorCount = originalVendorList.count { it.category == "decorator" }
        val djCount = originalVendorList.count { it.category == "dj" }
        val makeupCount = originalVendorList.count { it.category == "makeup" }

        binding.chipPhotographer.text = "Photographer ($photographerCount)"
        binding.chipCaterer.text = "Caterer ($catererCount)"
        binding.chipDecorator.text = "Decorator ($decoratorCount)"
        binding.chipDj.text = "DJ ($djCount)"
        binding.chipMakeup.text = "Makeup ($makeupCount)"
    }

    private fun openVendorDetail(vendor: Vendor) {
        val intent = Intent(this, VendorDetailActivity::class.java)
        intent.putExtra("vendorId", vendor.id)
        startActivity(intent)
    }

    private fun toggleFavoriteStatus(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                val updatedVendor = vendor.copy(
                    isFavorite = !vendor.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    val message = if (updatedVendor.isFavorite) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(this@VendorDirectoryActivity, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VendorDirectoryActivity, "Error updating vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite status", e)
                Toast.makeText(this@VendorDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bookVendor(vendor: Vendor) {
        if (vendor.isBooked) {
            Toast.makeText(this, "Vendor is already booked", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Book Vendor")
            .setMessage("Book ${vendor.getDisplayName()} for your wedding?\n\nThis will mark them as booked and you can manage contract details later.")
            .setPositiveButton("Book") { _, _ ->
                performVendorBooking(vendor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performVendorBooking(vendor: Vendor) {
        lifecycleScope.launch {
            try {
                val updatedVendor = vendor.copy(
                    isBooked = true,
                    updatedAt = System.currentTimeMillis()
                )

                val result = vendorRepository.updateVendor(updatedVendor)
                if (result.isSuccess) {
                    Toast.makeText(this@VendorDirectoryActivity, "${vendor.getDisplayName()} booked successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VendorDirectoryActivity, "Error booking vendor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error booking vendor", e)
                Toast.makeText(this@VendorDirectoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun callVendor(vendor: Vendor) {
        val phoneNumber = vendor.contactInfo.primaryPhone
        if (phoneNumber.isNotEmpty()) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewVendors.visibility = View.GONE

            val message = when {
                currentCategory != "all" -> "No ${currentCategory} vendors found"
                showFavoritesOnly -> "No favorite vendors found"
                showBookedOnly -> "No booked vendors found"
                originalVendorList.isEmpty() -> "No vendors added yet.\nStart by adding your first vendor!"
                else -> "No vendors match your current filters"
            }

            binding.tvEmptyMessage.text = message
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewVendors.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_vendor_directory, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_vendor_map -> {
                showVendorMap()
                true
            }
            R.id.action_export_vendors -> {
                exportVendorList()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showVendorMap() {
        // TODO: Implement vendor map view
        Toast.makeText(this, "Vendor map - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun exportVendorList() {
        // TODO: Implement vendor export
        Toast.makeText(this, "Export vendors - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadVendors()
    }
}
