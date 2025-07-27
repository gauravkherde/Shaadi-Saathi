package com.gaurav.shaadisaathi.utils

import java.text.SimpleDateFormat
import java.util.*

object ChatUtils {

    fun formatMessageTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatChatRoomTime(timestamp: Long): String {
        val today = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            today.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                    today.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            today.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                    today.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                "Yesterday"
            }
            else -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    fun truncateMessage(message: String, maxLength: Int = 50): String {
        return if (message.length > maxLength) {
            "${message.substring(0, maxLength)}..."
        } else {
            message
        }
    }

    fun generateChatRoomId(hostId: String, eventId: String? = null): String {
        return if (eventId != null) {
            "chat_${hostId}_${eventId}_${System.currentTimeMillis()}"
        } else {
            "chat_${hostId}_${System.currentTimeMillis()}"
        }
    }
}
