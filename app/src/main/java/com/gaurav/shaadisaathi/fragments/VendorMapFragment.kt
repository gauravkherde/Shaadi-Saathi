package com.gaurav.shaadisaathi.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.activities.VendorDetailActivity
import com.gaurav.shaadisaathi.databinding.FragmentVendorMapBinding
import com.gaurav.shaadisaathi.models.Vendor
import com.gaurav.shaadisaathi.repository.VendorRepository
import kotlinx.coroutines.launch

class VendorMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentVendorMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val vendorRepository = VendorRepository()
    private val vendorList = mutableListOf<Vendor>()
    private val vendorMarkers = mutableMapOf<Marker, Vendor>()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var selectedVendor: Vendor? = null
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL
    private val TAG = "VendorMapFragment"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val DEFAULT_ZOOM = 12f
        fun newInstance() = VendorMapFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVendorMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupBottomSheet()
        setupClickListeners()
        setupCategoryFilter()
        loadVendors()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetVendorDetails)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    selectedVendor = null
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Handle slide animation if needed
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.fabToggleMapType.setOnClickListener {
            toggleMapType()
        }

        binding.fabFilterVendors.setOnClickListener {
            showFilterDialog()
        }

        binding.etSearchVendors.setOnEditorActionListener { _, _, _ ->
            searchVendors(binding.etSearchVendors.text.toString())
            true
        }

        // Bottom sheet click listeners
        binding.bottomSheetVendorDetails.findViewById<View>(R.id.btnCloseBottomSheet)?.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.bottomSheetVendorDetails.findViewById<View>(R.id.btnCallVendor)?.setOnClickListener {
            selectedVendor?.let { callVendor(it) }
        }

        binding.bottomSheetVendorDetails.findViewById<View>(R.id.btnViewDetails)?.setOnClickListener {
            selectedVendor?.let { openVendorDetail(it) }
        }
    }

    private fun setupCategoryFilter() {
        binding.chipAll.setOnClickListener { filterVendorsByCategory("all") }
        binding.chipPhotographer.setOnClickListener { filterVendorsByCategory("photographer") }
        binding.chipCaterer.setOnClickListener { filterVendorsByCategory("caterer") }
        binding.chipDecorator.setOnClickListener { filterVendorsByCategory("decorator") }
        binding.chipFlorist.setOnClickListener { filterVendorsByCategory("florist") }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configure map settings
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false // We have custom location button
        }

        // Set map click listeners
        mMap.setOnMarkerClickListener { marker ->
            val vendor = vendorMarkers[marker]
            vendor?.let { showVendorBottomSheet(it) }
            true
        }

        mMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Enable location if permission granted
        enableMyLocation()

        // Set default location (India)
        val defaultLocation = LatLng(20.5937, 78.9629)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))

        getCurrentLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM))
            }
        }.addOnFailureListener {
            Log.e(TAG, "Error getting location", it)
            Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVendors() {
        binding.progressBarLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = vendorRepository.getAllVendors()
                if (result.isSuccess) {
                    val vendors = result.getOrNull() ?: emptyList()
                    vendorList.clear()
                    vendorList.addAll(vendors)
                    displayVendorsOnMap(vendors)
                    updateVendorCount(vendors.size)
                } else {
                    Log.e(TAG, "Error loading vendors: ${result.exceptionOrNull()}")
                    Toast.makeText(requireContext(), "Error loading vendors", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading vendors", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarLoading.visibility = View.GONE
            }
        }
    }

    private fun displayVendorsOnMap(vendors: List<Vendor>) {
        // Clear existing markers
        mMap.clear()
        vendorMarkers.clear()

        vendors.forEach { vendor ->
            val location = getVendorLocation(vendor)
            if (location != null) {
                val markerOptions = MarkerOptions()
                    .position(location)
                    .title(vendor.getDisplayName())
                    .snippet(vendor.getCategoryDisplayName())
                    .icon(getMarkerIcon(vendor.category))

                val marker = mMap.addMarker(markerOptions)
                marker?.let {
                    vendorMarkers[it] = vendor
                }
            }
        }
    }

    private fun getVendorLocation(vendor: Vendor): LatLng? {
        // For demo purposes, generate random locations around major Indian cities
        // In production, you'd use vendor's actual coordinates or geocode their address
        val cities = listOf(
            LatLng(28.6139, 77.2090), // Delhi
            LatLng(19.0760, 72.8777), // Mumbai
            LatLng(12.9716, 77.5946), // Bangalore
            LatLng(13.0827, 80.2707), // Chennai
            LatLng(22.5726, 88.3639), // Kolkata
            LatLng(18.5204, 73.8567), // Pune
            LatLng(21.1458, 79.0882)  // Nagpur
        )

        val baseCity = cities.random()
        val offsetLat = (Math.random() - 0.5) * 0.1 // ±0.05 degrees
        val offsetLng = (Math.random() - 0.5) * 0.1

        return LatLng(
            baseCity.latitude + offsetLat,
            baseCity.longitude + offsetLng
        )
    }

    private fun getMarkerIcon(category: String): BitmapDescriptorFactory {
        return when (category) {
            "photographer" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            "caterer" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            "decorator" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
            "florist" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            "dj" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
            "makeup" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }
    }

    private fun showVendorBottomSheet(vendor: Vendor) {
        selectedVendor = vendor

        // Populate bottom sheet with vendor data
        val vendorInitial = vendor.getDisplayName().firstOrNull()?.toString()?.uppercase() ?: "V"
        binding.bottomSheetVendorDetails.findViewById<android.widget.TextView>(R.id.tvVendorInitial)?.text = vendorInitial
        binding.bottomSheetVendorDetails.findViewById<android.widget.TextView>(R.id.tvVendorName)?.text = vendor.getDisplayName()
        binding.bottomSheetVendorDetails.findViewById<android.widget.TextView>(R.id.tvVendorCategory)?.text = vendor.getCategoryDisplayName()
        binding.bottomSheetVendorDetails.findViewById<android.widget.RatingBar>(R.id.ratingBarVendor)?.rating = vendor.rating.toFloat()

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun filterVendorsByCategory(category: String) {
        val filteredVendors = if (category == "all") {
            vendorList
        } else {
            vendorList.filter { it.category == category }
        }

        displayVendorsOnMap(filteredVendors)
        updateVendorCount(filteredVendors.size)

        // Update chip selection
        resetChipSelection()
        when (category) {
            "all" -> binding.chipAll.isChecked = true
            "photographer" -> binding.chipPhotographer.isChecked = true
            "caterer" -> binding.chipCaterer.isChecked = true
            "decorator" -> binding.chipDecorator.isChecked = true
            "florist" -> binding.chipFlorist.isChecked = true
        }
    }

    private fun resetChipSelection() {
        binding.chipAll.isChecked = false
        binding.chipPhotographer.isChecked = false
        binding.chipCaterer.isChecked = false
        binding.chipDecorator.isChecked = false
        binding.chipFlorist.isChecked = false
    }

    private fun searchVendors(query: String) {
        if (query.isEmpty()) {
            displayVendorsOnMap(vendorList)
            updateVendorCount(vendorList.size)
            return
        }

        val filteredVendors = vendorList.filter { vendor ->
            vendor.getDisplayName().contains(query, ignoreCase = true) ||
                    vendor.getCategoryDisplayName().contains(query, ignoreCase = true) ||
                    vendor.location.city.contains(query, ignoreCase = true) ||
                    vendor.services.contains(query, ignoreCase = true)
        }

        displayVendorsOnMap(filteredVendors)
        updateVendorCount(filteredVendors.size)
    }

    private fun toggleMapType() {
        currentMapType = when (currentMapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
            GoogleMap.MAP_TYPE_TERRAIN -> GoogleMap.MAP_TYPE_HYBRID
            else -> GoogleMap.MAP_TYPE_NORMAL
        }
        mMap.mapType = currentMapType
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf(
            "All Vendors",
            "Verified Only",
            "Available Only",
            "Favorites Only",
            "Nearby (5km)",
            "Nearby (10km)"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter Vendors")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> displayVendorsOnMap(vendorList)
                    1 -> filterByVerified()
                    2 -> filterByAvailable()
                    3 -> filterByFavorites()
                    4 -> filterByDistance(5.0)
                    5 -> filterByDistance(10.0)
                }
            }
            .show()
    }

    private fun filterByVerified() {
        val filteredVendors = vendorList.filter { it.isVerified }
        displayVendorsOnMap(filteredVendors)
        updateVendorCount(filteredVendors.size)
    }

    private fun filterByAvailable() {
        val filteredVendors = vendorList.filter { !it.isBooked }
        displayVendorsOnMap(filteredVendors)
        updateVendorCount(filteredVendors.size)
    }

    private fun filterByFavorites() {
        val filteredVendors = vendorList.filter { it.isFavorite }
        displayVendorsOnMap(filteredVendors)
        updateVendorCount(filteredVendors.size)
    }

    private fun filterByDistance(maxDistanceKm: Double) {
        // This would require actual location comparison
        // For demo, just show all vendors
        displayVendorsOnMap(vendorList)
        updateVendorCount(vendorList.size)
        Toast.makeText(requireContext(), "Filtering by ${maxDistanceKm}km radius", Toast.LENGTH_SHORT).show()
    }

    private fun updateVendorCount(count: Int) {
        if (count > 0) {
            binding.cardVendorCount.visibility = View.VISIBLE
            binding.tvVendorCount.text = "$count vendor${if (count > 1) "s" else ""} found"
        } else {
            binding.cardVendorCount.visibility = View.GONE
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

    private fun openVendorDetail(vendor: Vendor) {
        val intent = Intent(requireContext(), VendorDetailActivity::class.java)
        intent.putExtra("vendorId", vendor.id)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                    getCurrentLocation()
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
