package com.gaurav.shaadisaathi.repository

import com.gaurav.shaadisaathi.models.NotificationItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class NotificationRepository {

    private val database = FirebaseDatabase.getInstance()
    private val notificationsRef = database.getReference("notifications")

    suspend fun getNotifications(userId: String): Result<List<NotificationItem>> {
        return suspendCancellableCoroutine { continuation ->
            val userNotificationsRef = notificationsRef.child(userId)

            userNotificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val notifications = mutableListOf<NotificationItem>()

                        for (childSnapshot in snapshot.children) {
                            val notification = childSnapshot.getValue(NotificationItem::class.java)
                            notification?.let { notifications.add(it) }
                        }

                        // Sort by timestamp (newest first)
                        notifications.sortByDescending { it.timestamp }

                        continuation.resume(Result.success(notifications))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun addNotification(userId: String, notification: NotificationItem): Result<String> {
        return try {
            val notificationRef = notificationsRef.child(userId).child(notification.id)
            notificationRef.setValue(notification)
            Result.success(notification.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotification(notification: NotificationItem): Result<Boolean> {
        return try {
            // Assuming we need to find the user who owns this notification
            // In a real app, you might want to pass userId as parameter
            val updates = hashMapOf<String, Any>(
                "isRead" to notification.isRead,
                "timestamp" to notification.timestamp
            )

            // This is a simplified approach - you might need to structure this differently
            notificationsRef.child(notification.id).updateChildren(updates)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(userId: String, notificationId: String): Result<Boolean> {
        return try {
            val notificationRef = notificationsRef.child(userId).child(notificationId)
            notificationRef.child("isRead").setValue(true)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            val userNotificationsRef = notificationsRef.child(userId)

            userNotificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val updates = hashMapOf<String, Any>()

                        for (childSnapshot in snapshot.children) {
                            val notificationId = childSnapshot.key
                            if (notificationId != null) {
                                updates["$notificationId/isRead"] = true
                            }
                        }

                        if (updates.isNotEmpty()) {
                            userNotificationsRef.updateChildren(updates)
                                .addOnSuccessListener {
                                    continuation.resume(Result.success(true))
                                }
                                .addOnFailureListener { exception ->
                                    continuation.resume(Result.failure(exception))
                                }
                        } else {
                            continuation.resume(Result.success(true))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Boolean> {
        return try {
            val notificationRef = notificationsRef.child(userId).child(notificationId)
            notificationRef.removeValue()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllNotifications(userId: String): Result<Boolean> {
        return try {
            val userNotificationsRef = notificationsRef.child(userId)
            userNotificationsRef.removeValue()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(userId: String): Result<Int> {
        return suspendCancellableCoroutine { continuation ->
            val userNotificationsRef = notificationsRef.child(userId)

            userNotificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        var unreadCount = 0

                        for (childSnapshot in snapshot.children) {
                            val notification = childSnapshot.getValue(NotificationItem::class.java)
                            if (notification != null && !notification.isRead) {
                                unreadCount++
                            }
                        }

                        continuation.resume(Result.success(unreadCount))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    // Helper method to create different types of notifications
    suspend fun createInvitationNotification(
        userId: String,
        guestName: String,
        actionData: String
    ): Result<String> {
        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = "New RSVP Response",
            message = "$guestName has responded to your wedding invitation",
            type = "invitation",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = actionData,
            priority = "normal",
            icon = "ic_invitation"
        )

        return addNotification(userId, notification)
    }

    suspend fun createRSVPUpdateNotification(
        userId: String,
        guestName: String,
        status: String,
        actionData: String
    ): Result<String> {
        val statusText = when (status.lowercase()) {
            "confirmed" -> "confirmed their attendance"
            "declined" -> "declined the invitation"
            else -> "updated their RSVP status"
        }

        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = "RSVP Update",
            message = "$guestName has $statusText",
            type = "rsvp_update",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = actionData,
            priority = if (status == "confirmed") "normal" else "high",
            icon = "ic_check_circle"
        )

        return addNotification(userId, notification)
    }

    suspend fun createReminderNotification(
        userId: String,
        title: String,
        message: String,
        actionData: String
    ): Result<String> {
        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            type = "reminder",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = actionData,
            priority = "high",
            icon = "ic_reminder"
        )

        return addNotification(userId, notification)
    }

    suspend fun createChatNotification(
        userId: String,
        senderName: String,
        messagePreview: String,
        chatRoomId: String
    ): Result<String> {
        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = "New Message",
            message = "$senderName: $messagePreview",
            type = "chat",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = chatRoomId,
            priority = "normal",
            icon = "ic_chat"
        )

        return addNotification(userId, notification)
    }

    suspend fun createEventReminderNotification(
        userId: String,
        eventName: String,
        eventDate: String,
        eventId: String
    ): Result<String> {
        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = "Event Reminder",
            message = "Don't forget: $eventName is on $eventDate",
            type = "reminder",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = "event_$eventId",
            priority = "high",
            icon = "ic_event"
        )

        return addNotification(userId, notification)
    }

    suspend fun createVendorReminderNotification(
        userId: String,
        vendorName: String,
        reminderText: String,
        vendorId: String
    ): Result<String> {
        val notification = NotificationItem(
            id = System.currentTimeMillis().toString(),
            title = "Vendor Reminder",
            message = "$vendorName: $reminderText",
            type = "reminder",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = "vendor_$vendorId",
            priority = "normal",
            icon = "ic_vendor"
        )

        return addNotification(userId, notification)
    }

    // Real-time listener for notifications
    fun addNotificationListener(
        userId: String,
        onNotificationsChanged: (List<NotificationItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val notifications = mutableListOf<NotificationItem>()

                    for (childSnapshot in snapshot.children) {
                        val notification = childSnapshot.getValue(NotificationItem::class.java)
                        notification?.let { notifications.add(it) }
                    }

                    notifications.sortByDescending { it.timestamp }
                    onNotificationsChanged(notifications)
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(Exception(error.message))
            }
        }

        notificationsRef.child(userId).addValueEventListener(listener)
        return listener
    }

    fun removeNotificationListener(userId: String, listener: ValueEventListener) {
        notificationsRef.child(userId).removeEventListener(listener)
    }
}
