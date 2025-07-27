package com.gaurav.shaadisaathi.models

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "", // event, family, announcement, general
    val eventId: String = "", // linked to specific event if applicable
    val hostId: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSender: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val unreadCounts: Map<String, Int> = emptyMap() // userId -> unread count
) {
    constructor() : this("", "", "", "", "", "", emptyList(), "", 0L, "", 0L, true, emptyMap())
}
