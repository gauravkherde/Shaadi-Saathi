package com.gaurav.shaadisaathi.models

data class VoiceMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val voiceUrl: String = "",
    val duration: Long = 0L, // Duration in milliseconds
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isPlaying: Boolean = false,
    val waveform: List<Int> = emptyList() // For voice wave visualization
) {
    constructor() : this("", "", "", "", "", 0L, 0L, false, false, emptyList())
}
