package com.gaurav.shaadisaathi.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.utils.ImageUtils
import java.util.*

class PhotoUploadService : Service() {

    private val TAG = "PhotoUploadService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PhotoUploadService started")

        intent?.let { uploadIntent ->
            val imageUri = uploadIntent.getParcelableExtra<Uri>("imageUri")
            val albumId = uploadIntent.getStringExtra("albumId")
            val caption = uploadIntent.getStringExtra("caption") ?: ""

            if (imageUri != null && albumId != null) {
                uploadPhotoInBackground(imageUri, albumId, caption, startId)
            } else {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun uploadPhotoInBackground(imageUri: Uri, albumId: String, caption: String, startId: Int) {
        val currentUser = auth.currentUser ?: run {
            Log.e(TAG, "User not authenticated")
            stopSelf(startId)
            return
        }

        Log.d(TAG, "Starting background upload for album: $albumId")

        // Process image in background thread
        Thread {
            try {
                val compressedImage = ImageUtils.compressForFirestore(this, imageUri)

                if (compressedImage != null) {
                    savePhotoToFirestore(compressedImage, albumId, caption, currentUser.uid) {
                        stopSelf(startId)
                    }
                } else {
                    Log.e(TAG, "Failed to compress image")
                    stopSelf(startId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing image in background", e)
                stopSelf(startId)
            }
        }.start()
    }

    private fun savePhotoToFirestore(
        compressedImage: ImageUtils.CompressedImage,
        albumId: String,
        caption: String,
        uploaderId: String,
        onComplete: () -> Unit
    ) {
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
                        Log.d(TAG, "Photo saved to Firestore in background")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving photo to Firestore", e)
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user details", e)
                onComplete()
            }
    }
}
