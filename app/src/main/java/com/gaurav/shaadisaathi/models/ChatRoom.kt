package com.gaurav.shaadisaathi.models

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "group", // group, direct
    val eventId: String = "",
    val hostId: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSender: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val unreadCount: Int = 0
) {
    // Convert to ChatItem for adapter compatibility
    fun toChatItem(): ChatItem {
        return ChatItem(
            id = id,
            guestName = name,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            isOnline = isActive
        )
    }
}

// Keep ChatItem for adapter compatibility
data class ChatItem(
    val id: String = "",
    val guestName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)
