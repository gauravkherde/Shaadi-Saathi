package com.gaurav.shaadisaathi.models

data class ChatMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val messageType: String = "text", // text, image, voice, video
    val imageUrl: String = "",
    val voiceUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readBy: Map<String, Long> = emptyMap()
) {
    constructor() : this("", "", "", "", "", "text", "", "", 0L, false, emptyMap())
}
