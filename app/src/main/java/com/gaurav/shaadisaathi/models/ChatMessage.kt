package com.gaurav.shaadisaathi.models

data class ChatMessage(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false,
    val senderName: String = "",
    val senderId: String = "",
    val chatRoomId: String = "", // FIX: Add missing chatRoomId parameter
    val isDelivered: Boolean = true,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList(), // FIX: Add missing readBy parameter
    val messageType: String = "text", // text, image, voice
    val imageUrl: String = "",
    val voiceUrl: String = ""
)
