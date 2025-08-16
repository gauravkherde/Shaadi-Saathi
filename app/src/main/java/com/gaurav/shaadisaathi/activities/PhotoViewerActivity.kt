package com.gaurav.shaadisaathi.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.gaurav.shaadisaathi.databinding.ActivityPhotoViewerBinding

class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewerBinding

    private var photoId: String = ""
    private var photoBase64: String = "" // FIX: Use base64 instead of URL
    private var photoCaption: String = ""
    private var albumId: String = ""
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        photoId = intent.getStringExtra("photoId") ?: ""
        photoBase64 = intent.getStringExtra("photoBase64") ?: "" // FIX: Get base64 data
        photoCaption = intent.getStringExtra("photoCaption") ?: ""
        albumId = intent.getStringExtra("albumId") ?: ""
        position = intent.getIntExtra("position", 0)

        setupToolbar()
        loadPhoto()
    }

    private fun setupToolbar() {
        binding.tvPhotoTitle.text = "Photo ${position + 1}"
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadPhoto() {
        // FIX: Load photo from base64 instead of URL
        try {
            if (photoBase64.isNotEmpty()) {
                val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.touchImageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            // Handle error loading image
            binding.touchImageView.setImageResource(com.gaurav.shaadisaathi.R.drawable.ic_photo)
        }

        // Set caption if available
        if (photoCaption.isNotEmpty()) {
            binding.tvPhotoTitle.text = photoCaption
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
