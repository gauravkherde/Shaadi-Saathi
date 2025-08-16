package com.gaurav.shaadisaathi.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.PhotoAdapter
import com.gaurav.shaadisaathi.databinding.ActivityPhotoAlbumBinding
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.models.PhotoAlbum
import com.gaurav.shaadisaathi.repository.PhotoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class PhotoAlbumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoAlbumBinding
    private lateinit var photoAdapter: PhotoAdapter
    private val photoList = mutableListOf<Photo>()
    private val photoRepository = PhotoRepository()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var albumId: String = ""
    private var albumName: String = ""
    private var album: PhotoAlbum? = null
    private val TAG = "PhotoAlbumActivity"

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_REQUEST_CODE = 1002
        private const val GALLERY_REQUEST_CODE = 1003
    }

    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
            imageBitmap?.let { uploadImageFromCamera(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            uploadImagesFromGallery(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        albumId = intent.getStringExtra("albumId") ?: ""
        albumName = intent.getStringExtra("albumName") ?: "Photo Album"

        if (albumId.isEmpty()) {
            Toast.makeText(this, "Invalid album ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadAlbumDetails()
        loadPhotos()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = albumName
        }
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter(
            photos = photoList,
            onPhotoClick = { photo, position ->
                openPhotoViewer(photo, position)
            },
            onPhotoLongClick = { photo ->
                showPhotoOptions(photo)
            }
        )

        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(this@PhotoAlbumActivity, 3)
            adapter = photoAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPhoto.setOnClickListener {
            showAddPhotoOptions()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPhotos()
        }
    }

    private fun loadAlbumDetails() {
        lifecycleScope.launch {
            try {
                val result = photoRepository.getAlbumById(albumId)
                if (result.isSuccess) {
                    album = result.getOrNull()
                    album?.let { updateAlbumInfo(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading album details", e)
            }
        }
    }

    private fun updateAlbumInfo(album: PhotoAlbum) {
        binding.apply {
            tvAlbumDescription.text = if (album.description.isNotEmpty()) {
                album.description
            } else {
                "No description"
            }

            tvPhotoCount.text = "${album.photoCount} photos"
            tvCreatedBy.text = "Created by ${album.hostId}"

            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            tvCreatedDate.text = "Created ${dateFormat.format(java.util.Date(album.createdAt))}"
        }
    }

    private fun loadPhotos() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val result = photoRepository.getPhotosByAlbumId(albumId)
                if (result.isSuccess) {
                    val photos = result.getOrNull() ?: emptyList()
                    photoList.clear()
                    photoList.addAll(photos.sortedByDescending { it.uploadedAt })
                    photoAdapter.notifyDataSetChanged()

                    updateEmptyState(photos.isEmpty())
                    updatePhotoCount(photos.size)
                } else {
                    Toast.makeText(this@PhotoAlbumActivity, "Error loading photos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading photos", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showAddPhotoOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Choose Multiple")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Photos")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> checkStoragePermissionAndChoosePhoto()
                    2 -> checkStoragePermissionAndChooseMultiplePhotos()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }

    private fun checkStoragePermissionAndChoosePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            chooseFromGallery(false)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST_CODE)
        }
    }

    private fun checkStoragePermissionAndChooseMultiplePhotos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            chooseFromGallery(true)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST_CODE)
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun chooseFromGallery(multiple: Boolean) {
        if (multiple) {
            galleryLauncher.launch("image/*")
        } else {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }
    }

    private fun uploadImageFromCamera(bitmap: android.graphics.Bitmap) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Convert bitmap to URI and upload
                val uri = saveBitmapToCache(bitmap)
                uploadPhoto(uri, "camera_${System.currentTimeMillis()}.jpg")
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading camera image", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error uploading photo", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun uploadImagesFromGallery(uris: List<Uri>) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                uris.forEachIndexed { index, uri ->
                    uploadPhoto(uri, "gallery_${System.currentTimeMillis()}_$index.jpg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading gallery images", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error uploading photos", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun uploadPhoto(uri: Uri, fileName: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val storageRef = storage.reference.child("photos/$albumId/$fileName")

            val uploadTask = storageRef.putFile(uri)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    savePhotoToDatabase(downloadUrl.toString(), fileName, currentUser.displayName ?: "Unknown")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Error uploading photo", exception)
                Toast.makeText(this@PhotoAlbumActivity, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadPhoto", e)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun savePhotoToDatabase(imageUrl: String, fileName: String, uploaderName: String) {
        lifecycleScope.launch {
            try {
                val photo = Photo(
                    id = System.currentTimeMillis().toString(),
                    albumId = albumId,
                    uploaderId = auth.currentUser?.uid ?: "",
                    uploaderName = uploaderName,
                    uploadedAt = System.currentTimeMillis(),
                    caption = ""
                )

                val result = photoRepository.addPhoto(photo)
                if (result.isSuccess) {
                    photoList.add(0, photo) // Add to beginning
                    photoAdapter.notifyItemInserted(0)
                    binding.recyclerViewPhotos.scrollToPosition(0)

                    updateEmptyState(false)
                    updatePhotoCount(photoList.size)
                    updateAlbumPhotoCount()

                    Toast.makeText(this@PhotoAlbumActivity, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PhotoAlbumActivity, "Error saving photo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving photo to database", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveBitmapToCache(bitmap: android.graphics.Bitmap): Uri {
        val file = java.io.File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }

    private fun updateAlbumPhotoCount() {
        lifecycleScope.launch {
            try {
                album?.let { currentAlbum ->
                    val updatedAlbum = currentAlbum.copy(
                        photoCount = photoList.size
                    )
                    photoRepository.updateAlbum(updatedAlbum)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating album photo count", e)
            }
        }
    }

    private fun openPhotoViewer(photo: Photo, position: Int) {
        val intent = Intent(this, PhotoViewerActivity::class.java).apply {
            putExtra("photoId", photo.id)
            putExtra("photoCaption", photo.caption)
            putExtra("albumId", albumId)
            putExtra("position", position)
        }
        startActivity(intent)
    }

    private fun showPhotoOptions(photo: Photo) {
        val options = if (photo.uploaderId == auth.currentUser?.uid) {
            arrayOf("View", "Add Caption", "Share", "Delete")
        } else {
            arrayOf("View", "Share")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Photo Options")
            .setItems(options) { _, which ->
                when {
                    options[which] == "View" -> openPhotoViewer(photo, photoList.indexOf(photo))
                    options[which] == "Add Caption" -> showAddCaptionDialog(photo)
                    options[which] == "Share" -> sharePhoto(photo)
                    options[which] == "Delete" -> showDeleteConfirmation(photo)
                }
            }
            .show()
    }

    private fun showAddCaptionDialog(photo: Photo) {
        val input = android.widget.EditText(this)
        input.setText(photo.caption)
        input.hint = "Enter caption"

        androidx.appcompat.app.AlertDialog.Builder(this)
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
        lifecycleScope.launch {
            try {
                val updatedPhoto = photo.copy(
                    caption = caption
                )

                val result = photoRepository.updatePhoto(updatedPhoto)
                if (result.isSuccess) {
                    val index = photoList.indexOfFirst { it.id == photo.id }
                    if (index != -1) {
                        photoList[index] = updatedPhoto
                        photoAdapter.notifyItemChanged(index)
                    }
                    Toast.makeText(this@PhotoAlbumActivity, "Caption updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PhotoAlbumActivity, "Error updating caption", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating caption", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sharePhoto(photo: Photo) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this photo from our wedding album!")
            putExtra(Intent.EXTRA_SUBJECT, "Wedding Photo - $albumName")
        }
        startActivity(Intent.createChooser(intent, "Share Photo"))
    }

    private fun showDeleteConfirmation(photo: Photo) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto(photo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePhoto(photo: Photo) {
        lifecycleScope.launch {
            try {
                val result = photoRepository.deletePhoto(photo.id)
                if (result.isSuccess) {
                    // Remove from list
                    val index = photoList.indexOfFirst { it.id == photo.id }
                    if (index != -1) {
                        photoList.removeAt(index)
                        photoAdapter.notifyItemRemoved(index)
                    }

                    updateEmptyState(photoList.isEmpty())
                    updatePhotoCount(photoList.size)
                    updateAlbumPhotoCount()

                    Toast.makeText(this@PhotoAlbumActivity, "Photo deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PhotoAlbumActivity, "Error deleting photo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting photo", e)
                Toast.makeText(this@PhotoAlbumActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewPhotos.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewPhotos.visibility = View.VISIBLE
        }
    }

    private fun updatePhotoCount(count: Int) {
        binding.tvPhotoCount.text = "$count photo${if (count != 1) "s" else ""}"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    uploadPhoto(uri, "gallery_${System.currentTimeMillis()}.jpg")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseFromGallery(false)
                } else {
                    Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_upload -> {
                showAddPhotoOptions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareAlbum() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out our wedding photo album: $albumName")
            putExtra(Intent.EXTRA_SUBJECT, "Wedding Photo Album - $albumName")
        }
        startActivity(Intent.createChooser(intent, "Share Album"))
    }

    private fun openAlbumSettings() {
        // TODO: Implement album settings
        Toast.makeText(this, "Album settings - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
