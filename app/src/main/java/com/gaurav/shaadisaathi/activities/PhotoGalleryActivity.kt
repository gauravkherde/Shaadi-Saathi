package com.gaurav.shaadisaathi.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.adapters.PhotoAdapter
import com.gaurav.shaadisaathi.adapters.PhotoGridAdapter
import com.gaurav.shaadisaathi.databinding.ActivityPhotoGalleryBinding
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoGalleryBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photoGridAdapter: PhotoGridAdapter
    private val photoList = mutableListOf<Photo>()
    private var albumId = ""
    private var albumName = ""
    private var isGridView = true
    private val TAG = "PhotoGalleryActivity"

    // Activity launchers declared as class properties
    private val cameraLauncher: ActivityResultLauncher<Uri> = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                Log.d(TAG, "✅ Camera photo taken successfully: $uri")
                processLargeImage(uri)
            }
        } else {
            Log.d(TAG, "❌ Camera photo was cancelled")
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d(TAG, "✅ Gallery photo selected: $it")
            processLargeImage(it)
        } ?: run {
            Log.d(TAG, "❌ Gallery selection was cancelled")
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Permission result: $isGranted")

        if (isGranted) {
            when (pendingAction) {
                "camera" -> {
                    Log.d(TAG, "Permission granted, taking photo")
                    executeCamera()
                }
                "gallery" -> {
                    Log.d(TAG, "Permission granted, opening gallery")
                    executeGallery()
                }
            }
        } else {
            Log.d(TAG, "Permission denied")
            showPermissionDeniedDialog()
        }
        pendingAction = ""
    }

    private val multiplePermissionsLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val storageGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true

        Log.d(TAG, "Multiple permissions result - Camera: $cameraGranted, Storage: $storageGranted")

        if (cameraGranted && storageGranted) {
            executeCamera()
        } else {
            showPermissionDeniedDialog()
        }
        pendingAction = ""
    }

    private var currentPhotoUri: Uri? = null
    private var pendingAction: String = ""

    companion object {
        private val REQUIRED_CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        albumId = intent.getStringExtra("albumId") ?: "default_album"
        albumName = intent.getStringExtra("albumName") ?: "Wedding Photos"

        binding.tvGalleryTitle.text = albumName

        // Debug Firestore authentication first
        debugFirestoreAuthentication()

        setupRecyclerView()
        setupClickListeners()
        loadPhotos()

        Log.d(TAG, "PhotoGalleryActivity created for album: $albumName")
    }

    private fun debugFirestoreAuthentication() {
        val currentUser = auth.currentUser
        Log.d(TAG, "=== FIRESTORE AUTHENTICATION DEBUG ===")

        if (currentUser != null) {
            Log.d(TAG, "✅ User authenticated for Firestore")
            Log.d(TAG, "User ID: ${currentUser.uid}")
            Log.d(TAG, "User email: ${currentUser.email}")
            Log.d(TAG, "Email verified: ${currentUser.isEmailVerified}")

            // Test a simple Firestore read
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    Log.d(TAG, "✅ Firestore test read successful")
                    Log.d(TAG, "User document exists: ${doc.exists()}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Firestore test read failed: ${e.message}")
                    showFirestorePermissionErrorDialog()
                }
        } else {
            Log.e(TAG, "❌ User NOT authenticated for Firestore")
            Toast.makeText(this, "Please login to access photos", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.fabAddPhoto.setOnClickListener {
            Log.d(TAG, "🎯 FAB Add Photo clicked")
            showPhotoOptionsDialog()
        }

        binding.btnToggleView.setOnClickListener {
            toggleViewMode()
        }
    }

    private fun setupRecyclerView() {
        photoGridAdapter = PhotoGridAdapter(photoList) { photo ->
            openPhotoViewer(photo)
        }

        photoAdapter = PhotoAdapter(
            photos = photoList,
            onPhotoClick = { photo, position ->
                openPhotoViewer(photo)
            },
            onPhotoLongClick = { photo ->
                showPhotoOptions(photo)
            }
        )

        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(this@PhotoGalleryActivity, 3)
            adapter = photoGridAdapter
        }
    }

    private fun toggleViewMode() {
        isGridView = !isGridView

        if (isGridView) {
            binding.recyclerViewPhotos.apply {
                layoutManager = GridLayoutManager(this@PhotoGalleryActivity, 3)
                adapter = photoGridAdapter
            }
            binding.btnToggleView.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.recyclerViewPhotos.apply {
                layoutManager = LinearLayoutManager(this@PhotoGalleryActivity)
                adapter = photoAdapter
            }
            binding.btnToggleView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun loadPhotos() {
        if (albumId.isEmpty() || isFinishing || isDestroyed) return

        binding.progressBar.visibility = View.VISIBLE

        Log.d(TAG, "Loading photos for album: $albumId")

        // Check authentication before querying
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated, cannot load photos")
            binding.progressBar.visibility = View.GONE

            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "Please login to view photos", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            return
        }

        Log.d(TAG, "User authenticated: ${currentUser.uid}, loading photos...")

        firestore.collection("photos")
            .whereEqualTo("albumId", albumId)
            .addSnapshotListener { snapshots, e ->
                // Always check activity state in Firestore callbacks
                if (isFinishing || isDestroyed) {
                    Log.w(TAG, "Activity finishing/destroyed, ignoring Firestore callback")
                    return@addSnapshotListener
                }

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    binding.progressBar.visibility = View.GONE

                    if (e != null) {
                        Log.e(TAG, "Error loading photos", e)

                        when {
                            e.message?.contains("PERMISSION_DENIED") == true -> {
                                showFirestorePermissionErrorDialog()
                            }
                            e.message?.contains("permission") == true -> {
                                Toast.makeText(this@PhotoGalleryActivity, "Permission denied. Check Firestore rules.", Toast.LENGTH_LONG).show()
                            }
                            e.message?.contains("Missing or insufficient permissions") == true -> {
                                showFirestorePermissionErrorDialog()
                            }
                            else -> {
                                Toast.makeText(this@PhotoGalleryActivity, "Error loading photos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return@runOnUiThread
                    }

                    photoList.clear()
                    val validPhotos = mutableListOf<Photo>()

                    snapshots?.documents?.forEach { doc ->
                        try {
                            val photo = doc.toObject(Photo::class.java)
                            photo?.let {
                                if (it.isApproved) {
                                    validPhotos.add(it)
                                }
                                Log.d(TAG, "Loaded photo: ${it.id}")
                            }
                        } catch (ex: Exception) {
                            Log.w(TAG, "Skipping invalid photo document: ${doc.id}")
                        }
                    }

                    validPhotos.sortByDescending { it.uploadedAt }
                    photoList.addAll(validPhotos)

                    try {
                        photoGridAdapter.notifyDataSetChanged()
                        photoAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error updating UI", ex)
                    }

                    Log.d(TAG, "Total photos loaded: ${photoList.size}")
                }
            }
    }

    private fun showFirestorePermissionErrorDialog() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing/destroyed, cannot show Firestore permission dialog")
            return
        }

        try {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this)
                        .setTitle("Firestore Permission Error")
                        .setMessage("Cannot access wedding photos due to database security rules.\n\nThis means:\n• Firestore rules need updating\n• User authentication may have expired\n\nPlease check Firebase Console → Firestore Database → Rules")
                        .setPositiveButton("Retry") { _, _ ->
                            if (!isFinishing && !isDestroyed) {
                                loadPhotos()
                            }
                        }
                        .setNegativeButton("Close") { _, _ ->
                            if (!isFinishing && !isDestroyed) {
                                finish()
                            }
                        }
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Firestore permission dialog", e)
        }
    }

    private fun showPhotoOptions(photo: Photo) {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing/destroyed, cannot show photo options dialog")
            return
        }

        try {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    val options = if (photo.uploaderId == auth.currentUser?.uid) {
                        arrayOf("View", "Add Caption", "Share", "Delete")
                    } else {
                        arrayOf("View", "Share")
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Photo Options")
                        .setItems(options) { _, which ->
                            when {
                                options[which] == "View" -> openPhotoViewer(photo)
                                options[which] == "Add Caption" -> showAddCaptionDialog(photo)
                                options[which] == "Share" -> sharePhoto(photo)
                                options[which] == "Delete" -> showDeleteConfirmation(photo)
                            }
                        }
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing photo options", e)
            Toast.makeText(this, "Cannot show photo options at this time", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhotoOptionsDialog() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing/destroyed, cannot show photo options dialog")
            return
        }

        Log.d(TAG, "📸 Showing photo options dialog")

        try {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this)
                        .setTitle("📸 Add Wedding Photo")
                        .setMessage("Photos up to 10MB will be compressed and stored securely.\n\nSelect your photo source:")
                        .setPositiveButton("📷 Take Photo") { dialogInterface, _ ->
                            Log.d(TAG, "✅ Camera button clicked")
                            dialogInterface.dismiss()
                            if (!isFinishing && !isDestroyed) {
                                handleCameraAction()
                            }
                        }
                        .setNeutralButton("🖼️ Gallery") { dialogInterface, _ ->
                            Log.d(TAG, "✅ Gallery button clicked")
                            dialogInterface.dismiss()
                            if (!isFinishing && !isDestroyed) {
                                handleGalleryAction()
                            }
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ ->
                            Log.d(TAG, "❌ Cancel button clicked")
                            dialogInterface.dismiss()
                        }
                        .setCancelable(true)
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing photo dialog", e)
            Toast.makeText(this, "Cannot show photo options at this time", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCameraAction() {
        Log.d(TAG, "🎯 Handling camera action")
        pendingAction = "camera"

        if (isCameraPermissionGranted()) {
            Log.d(TAG, "✅ Camera permission already granted")
            executeCamera()
        } else {
            Log.d(TAG, "❌ Requesting camera permissions")
            requestCameraPermissions()
        }
    }

    private fun handleGalleryAction() {
        Log.d(TAG, "🎯 Handling gallery action")
        pendingAction = "gallery"

        if (isStoragePermissionGranted()) {
            Log.d(TAG, "✅ Storage permission already granted")
            executeGallery()
        } else {
            Log.d(TAG, "❌ Requesting storage permission")
            requestStoragePermission()
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Camera permission check - Camera: $cameraGranted, Storage: $storageGranted")
        return cameraGranted && storageGranted
    }

    private fun isStoragePermissionGranted(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Storage permission check: $granted")
        return granted
    }

    private fun requestCameraPermissions() {
        Log.d(TAG, "🔐 Requesting camera permissions...")

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showPermissionRationale("Camera", "to take wedding photos") {
                multiplePermissionsLauncher.launch(REQUIRED_CAMERA_PERMISSIONS)
            }
        } else {
            multiplePermissionsLauncher.launch(REQUIRED_CAMERA_PERMISSIONS)
        }
    }

    private fun requestStoragePermission() {
        Log.d(TAG, "🔐 Requesting storage permission...")

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showPermissionRationale("Storage", "to access your photos") {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showPermissionRationale(permissionName: String, purpose: String, onProceed: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("$permissionName Permission Needed")
            .setMessage("ShaadiSaathi needs $permissionName permission $purpose for your wedding album.\n\nPlease grant the permission in the next dialog.")
            .setPositiveButton("Grant Permission") { _, _ ->
                onProceed()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeCamera() {
        Log.d(TAG, "🎯 Executing camera...")
        try {
            val photoFile = createImageFile()
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )

            Log.d(TAG, "📸 Launching camera with URI: $currentPhotoUri")
            cameraLauncher.launch(currentPhotoUri)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing camera", e)
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun executeGallery() {
        Log.d(TAG, "🎯 Executing gallery...")
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing gallery", e)
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("WEDDING_${timeStamp}_", ".jpg", storageDir)
    }

    private fun showPermissionDeniedDialog() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing/destroyed, cannot show permission denied dialog")
            return
        }

        try {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("ShaadiSaathi needs permission to upload wedding photos.\n\nPlease enable permissions in Settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            if (!isFinishing && !isDestroyed) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing permission denied dialog", e)
        }
    }

    private fun processLargeImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return

        Log.d(TAG, "Processing image: $imageUri")

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Processing Wedding Photo")
            .setMessage("Compressing and preparing your photo...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val compressedImage = ImageUtils.compressForFirestore(this, imageUri)

                runOnUiThread {
                    if (compressedImage != null) {
                        progressDialog.setMessage("Uploading to wedding album...")
                        saveCompressedPhotoToFirestore(compressedImage, currentUser.uid)
                        progressDialog.dismiss()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to process photo", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Log.e(TAG, "Error processing image", e)
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error processing photo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun saveCompressedPhotoToFirestore(
        compressedImage: ImageUtils.CompressedImage,
        uploaderId: String
    ) {
        firestore.collection("users").document(uploaderId).get()
            .addOnSuccessListener { userDoc ->
                val uploaderName = userDoc.getString("name") ?: "Anonymous"
                val photoId = UUID.randomUUID().toString()

                val photo = Photo(
                    id = photoId,
                    albumId = albumId,
                    uploaderId = uploaderId,
                    uploaderName = uploaderName,
                    imageBase64 = compressedImage.fullImageBase64,
                    thumbnailBase64 = compressedImage.thumbnailBase64,
                    caption = "",
                    uploadedAt = System.currentTimeMillis(),
                    isApproved = true,
                    originalWidth = compressedImage.originalWidth,
                    originalHeight = compressedImage.originalHeight,
                    compressedSize = compressedImage.compressedSize,
                    fileSize = "${compressedImage.compressedSize / 1024} KB"
                )

                firestore.collection("photos").document(photoId).set(photo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Wedding photo uploaded successfully! 🎉\nSize: ${photo.fileSize}", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Photo saved successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving photo", e)
                        when {
                            e.message?.contains("PERMISSION_DENIED") == true -> {
                                showFirestorePermissionErrorDialog()
                            }
                            else -> {
                                Toast.makeText(this, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user data", e)
                Toast.makeText(this, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun likePhoto(photo: Photo) {
        val currentUser = auth.currentUser ?: return
        val photoRef = firestore.collection("photos").document(photo.id)

        val updatedLikes = photo.likes.toMutableMap()

        if (updatedLikes.containsKey(currentUser.uid)) {
            updatedLikes.remove(currentUser.uid)
            Toast.makeText(this, "Photo unliked", Toast.LENGTH_SHORT).show()
        } else {
            updatedLikes[currentUser.uid] = true
            Toast.makeText(this, "Photo liked! ❤️", Toast.LENGTH_SHORT).show()
        }

        photoRef.update("likes", updatedLikes)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating likes", e)
                Toast.makeText(this, "Error updating like", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openComments(photo: Photo) {
        Toast.makeText(this, "Comments feature - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun openPhotoViewer(photo: Photo) {
        val intent = Intent(this, PhotoViewerActivity::class.java)
        intent.putExtra("photoId", photo.id)
        intent.putExtra("photoBase64", photo.imageBase64)
        intent.putExtra("albumId", albumId)
        startActivity(intent)
    }

    private fun showAddCaptionDialog(photo: Photo) {
        val input = android.widget.EditText(this)
        input.setText(photo.caption)
        input.hint = "Enter caption"

        AlertDialog.Builder(this)
            .setTitle("Add Caption")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val caption = input.text.toString().trim()
                updatePhotoCaption(photo, caption)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePhotoCaption(photo: Photo, caption: String) {
        val photoRef = firestore.collection("photos").document(photo.id)
        val updatedPhoto = photo.copy(caption = caption)

        photoRef.set(updatedPhoto)
            .addOnSuccessListener {
                Toast.makeText(this, "Caption updated", Toast.LENGTH_SHORT).show()
                // Refresh the photo list
                loadPhotos()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating caption", e)
                Toast.makeText(this, "Error updating caption: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sharePhoto(photo: Photo) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this photo from our wedding album!")
            putExtra(Intent.EXTRA_SUBJECT, "Wedding Photo")
        }
        startActivity(Intent.createChooser(intent, "Share Photo"))
    }

    private fun showDeleteConfirmation(photo: Photo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto(photo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePhoto(photo: Photo) {
        val photoRef = firestore.collection("photos").document(photo.id)
        
        photoRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
                // Refresh the photo list
                loadPhotos()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting photo", e)
                Toast.makeText(this, "Error deleting photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (photoList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewPhotos.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewPhotos.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    override fun onDestroy() {
        Log.d(TAG, "PhotoGalleryActivity being destroyed")

        try {
            // Cancel any pending operations
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }

        super.onDestroy()
    }
}
