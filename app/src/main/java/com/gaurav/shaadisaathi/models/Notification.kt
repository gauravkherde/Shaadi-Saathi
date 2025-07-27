package com.gaurav.shaadisaathi.models

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // chat, photo, event, rsvp
    val relatedId: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
) {
    constructor() : this("", "", "", "", "", "", false, 0L, "")
}
