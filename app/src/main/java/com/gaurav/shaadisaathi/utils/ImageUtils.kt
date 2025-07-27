package com.gaurav.shaadisaathi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageUtils {

    // Compression settings for Firestore (1MB document limit)
    private const val MAX_ORIGINAL_SIZE = 10_000_000 // 10MB max input
    private const val MAX_COMPRESSED_SIZE = 800_000  // ~800KB for Firestore safety
    private const val THUMBNAIL_SIZE = 50_000       // ~50KB for thumbnails
    private const val CHAT_IMAGE_SIZE = 400_000     // ~400KB for chat images

    /**
     * Compress large image (up to 10MB) for Firestore storage
     */
    fun compressForFirestore(context: Context, imageUri: Uri): CompressedImage? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Fix image orientation
            val rotatedBitmap = fixImageOrientation(context, imageUri, originalBitmap)

            // Create different quality versions
            val highQuality = compressToTargetSize(rotatedBitmap, MAX_COMPRESSED_SIZE, 85)
            val thumbnail = compressToTargetSize(rotatedBitmap, THUMBNAIL_SIZE, 60)

            CompressedImage(
                fullImageBase64 = bitmapToBase64(highQuality.bitmap, highQuality.quality),
                thumbnailBase64 = bitmapToBase64(thumbnail.bitmap, thumbnail.quality),
                originalWidth = rotatedBitmap.width,
                originalHeight = rotatedBitmap.height,
                compressedSize = estimateBase64Size(highQuality.bitmap, highQuality.quality)
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decompress Base64 to high-quality bitmap for viewing
     */
    fun decompressForViewing(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Smart compression that maintains quality while hitting target size
     */
    private fun compressToTargetSize(bitmap: Bitmap, targetSizeBytes: Int, initialQuality: Int): CompressionResult {
        var quality = initialQuality
        var compressedBitmap = bitmap
        var currentSize: Int

        // First, scale down if image is too large
        val scaleFactor = calculateScaleFactor(bitmap, targetSizeBytes)
        if (scaleFactor < 1.0f) {
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()
            compressedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // Then compress by quality until we hit target size
        do {
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            currentSize = outputStream.size()

            if (currentSize > targetSizeBytes && quality > 30) {
                quality -= 5
            } else {
                break
            }
        } while (quality > 30)

        return CompressionResult(compressedBitmap, quality, currentSize)
    }

    /**
     * Calculate scale factor to reduce image dimensions
     */
    private fun calculateScaleFactor(bitmap: Bitmap, targetSizeBytes: Int): Float {
        val currentPixels = bitmap.width * bitmap.height
        val maxPixels = targetSizeBytes / 3 // Rough estimate: 3 bytes per pixel

        return if (currentPixels > maxPixels) {
            kotlin.math.sqrt(maxPixels.toFloat() / currentPixels)
        } else {
            1.0f
        }
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Convert bitmap to Base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Estimate Base64 string size
     */
    private fun estimateBase64Size(bitmap: Bitmap, quality: Int): Int {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val compressedSize = outputStream.size()
        return (compressedSize * 1.37).toInt() // Base64 adds ~37% overhead
    }

    // Data classes
    data class CompressedImage(
        val fullImageBase64: String,
        val thumbnailBase64: String,
        val originalWidth: Int,
        val originalHeight: Int,
        val compressedSize: Int
    )

    private data class CompressionResult(
        val bitmap: Bitmap,
        val quality: Int,
        val sizeBytes: Int
    )
}
