package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gaurav.shaadisaathi.databinding.ActivityPhotoViewerBinding
import com.gaurav.shaadisaathi.utils.ImageUtils
import kotlinx.coroutines.*

class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewerBinding
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageBase64 = intent.getStringExtra("imageBase64") ?: ""
        val photoId = intent.getStringExtra("photoId") ?: ""

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            // TODO: Share functionality
        }

        binding.btnLike.setOnClickListener {
            // TODO: Like functionality
        }

        binding.btnComment.setOnClickListener {
            // TODO: Comment functionality
        }

        binding.btnDownload.setOnClickListener {
            // TODO: Download functionality
        }

        // Load high-quality image
        if (imageBase64.isNotEmpty()) {
            loadHighQualityImage(imageBase64)
        }
    }

    private fun loadHighQualityImage(imageBase64: String) {
        binding.progressBar.visibility = View.VISIBLE

        activityScope.launch {
            try {
                val highQualityBitmap = withContext(Dispatchers.IO) {
                    ImageUtils.decompressForViewing(imageBase64)
                }

                if (highQualityBitmap != null) {
                    binding.touchImageView.setImageBitmap(highQualityBitmap)
                    binding.progressBar.visibility = View.GONE
                } else {
                    throw Exception("Failed to decompress image")
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.touchImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
