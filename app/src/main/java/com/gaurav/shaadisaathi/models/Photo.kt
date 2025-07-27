package com.gaurav.shaadisaathi.models

data class Photo(
    val id: String = "",
    val albumId: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val imageBase64: String = "", // High-quality compressed image (up to 800KB)
    val thumbnailBase64: String = "", // Small thumbnail for gallery view (~50KB)
    val caption: String = "",
    val likes: Map<String, Boolean> = emptyMap(),
    val comments: List<PhotoComment> = emptyList(),
    val uploadedAt: Long = System.currentTimeMillis(),
    val isApproved: Boolean = true,
    // NEW: Enhanced metadata for better image handling
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val compressedSize: Int = 0, // Size after compression in bytes
    val compressionQuality: Int = 85, // Quality used for compression (30-100)
    val fileSize: String = "", // Human-readable file size (e.g., "756 KB")
    val imageFormat: String = "JPEG", // Image format (JPEG/PNG)
    val cameraInfo: CameraInfo = CameraInfo() // Camera/device info
) {
    constructor() : this(
        "", "", "", "", "", "", "", emptyMap(), emptyList(), 0L, true,
        0, 0, 0, 85, "", "JPEG", CameraInfo()
    )
}

data class PhotoComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Map<String, Boolean> = emptyMap() // NEW: Allow liking comments
) {
    constructor() : this("", "", "", "", 0L, emptyMap())
}

// NEW: Camera/device information for photos
data class CameraInfo(
    val deviceModel: String = "",
    val cameraType: String = "", // "front" or "back" or "gallery"
    val location: String = "", // Optional: GPS location if enabled
    val timestamp: String = "" // Human-readable timestamp
) {
    constructor() : this("", "", "", "")
}
