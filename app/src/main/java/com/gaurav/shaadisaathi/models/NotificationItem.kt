package com.gaurav.shaadisaathi.models

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // invitation, rsvp_update, reminder, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionData: String = "", // Additional data for handling clicks
    val priority: String = "normal", // high, normal, low
    val icon: String = "" // Icon identifier
)

