package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityPhotoUploadBinding
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.utils.ImageUtils
import java.util.*

class PhotoUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoUploadBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null
    private var albumId = ""
    private var albumName = ""
    private val TAG = "PhotoUploadActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Get intent data
        albumId = intent.getStringExtra("albumId") ?: "default_album"
        albumName = intent.getStringExtra("albumName") ?: "Wedding Photos"
        selectedImageUri = intent.getParcelableExtra("imageUri")

        binding.tvAlbumName.text = "Upload to: $albumName"

        setupClickListeners()
        loadSelectedImage()

        Log.d(TAG, "PhotoUploadActivity created for album: $albumName")
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnUpload.setOnClickListener {
            uploadPhoto()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadSelectedImage() {
        selectedImageUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivSelectedPhoto)

            binding.layoutPhotoPreview.visibility = View.VISIBLE
            binding.layoutSelectPhoto.visibility = View.GONE
        } ?: run {
            binding.layoutPhotoPreview.visibility = View.GONE
            binding.layoutSelectPhoto.visibility = View.VISIBLE

            binding.btnSelectPhoto.setOnClickListener {
                openImagePicker()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    private fun uploadPhoto() {
        val imageUri = selectedImageUri
        if (imageUri == null) {
            Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = binding.etCaption.text.toString().trim()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false
        binding.btnUpload.text = "Uploading..."

        Log.d(TAG, "Starting photo upload...")

        // Process image in background thread
        Thread {
            try {
                val compressedImage = ImageUtils.compressForFirestore(this, imageUri)

                runOnUiThread {
                    if (compressedImage != null) {
                        savePhotoToFirestore(compressedImage, caption, currentUser.uid)
                    } else {
                        handleUploadError("Failed to compress image", null)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    handleUploadError("Error processing image", e)
                }
            }
        }.start()
    }

    private fun savePhotoToFirestore(
        compressedImage: ImageUtils.CompressedImage,
        caption: String,
        uploaderId: String
    ) {
        // Get current user's name
        firestore.collection("users").document(uploaderId).get()
            .addOnSuccessListener { userDoc ->
                val uploaderName = userDoc.getString("name") ?: "Anonymous"
                val photoId = UUID.randomUUID().toString()

                // FIXED: Use the updated Photo model with correct parameters
                val photo = Photo(
                    id = photoId,
                    albumId = albumId,
                    uploaderId = uploaderId,
                    uploaderName = uploaderName,
                    imageBase64 = compressedImage.fullImageBase64, // Fixed: was imageUrl
                    thumbnailBase64 = compressedImage.thumbnailBase64, // Fixed: was thumbnailUrl
                    caption = caption,
                    uploadedAt = System.currentTimeMillis(),
                    isApproved = true,
                    originalWidth = compressedImage.originalWidth,
                    originalHeight = compressedImage.originalHeight,
                    compressedSize = compressedImage.compressedSize,
                    fileSize = "${compressedImage.compressedSize / 1024} KB"
                )

                firestore.collection("photos").document(photoId).set(photo)
                    .addOnSuccessListener {
                        handleUploadSuccess()
                    }
                    .addOnFailureListener { e ->
                        handleUploadError("Error saving photo details", e)
                    }
            }
            .addOnFailureListener { e ->
                handleUploadError("Error getting user details", e)
            }
    }

    private fun handleUploadSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.tvUploadProgress.visibility = View.GONE
        binding.btnUpload.isEnabled = true
        binding.btnUpload.text = "Upload Photo"

        Toast.makeText(this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Photo upload completed successfully")

        // Return to gallery
        setResult(RESULT_OK)
        finish()
    }

    private fun handleUploadError(message: String, exception: Exception? = null) {
        binding.progressBar.visibility = View.GONE
        binding.tvUploadProgress.visibility = View.GONE
        binding.btnUpload.isEnabled = true
        binding.btnUpload.text = "Upload Photo"

        Log.e(TAG, message, exception)
        Toast.makeText(this, "$message: ${exception?.message}", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                loadSelectedImage()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}
