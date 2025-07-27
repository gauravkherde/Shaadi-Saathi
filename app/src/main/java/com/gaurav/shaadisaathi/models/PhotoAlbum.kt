package com.gaurav.shaadisaathi.models

data class PhotoAlbum(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val eventId: String = "",
    val hostId: String = "",
    val coverImageUrl: String = "",
    val photoCount: Int = 0,
    val contributors: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true
) {
    constructor() : this("", "", "", "", "", "", 0, emptyList(), 0L, true)
}
