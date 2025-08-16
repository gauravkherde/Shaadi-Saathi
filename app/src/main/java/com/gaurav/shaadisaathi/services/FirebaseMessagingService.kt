package com.gaurav.shaadisaathi.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gaurav.shaadisaathi.utils.NotificationUtils

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    override fun onCreate() {
        super.onCreate()
        // Create notification channels when service is created
        NotificationUtils.createNotificationChannels(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            handleNotificationMessage(it)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]

        try {
            when (type) {
                "chat" -> {
                    val chatRoomId = data["chatRoomId"] ?: return
                    val chatRoomName = data["chatRoomName"] ?: return
                    val senderName = data["senderName"] ?: return
                    val message = data["message"] ?: return

                    NotificationUtils.showChatNotification(
                        this,
                        chatRoomId,
                        chatRoomName,
                        senderName,
                        message
                    )
                }

                "photo" -> {
                    val albumId = data["albumId"] ?: return
                    val albumName = data["albumName"] ?: return
                    val uploaderName = data["uploaderName"] ?: return

                    NotificationUtils.showPhotoNotification(
                        this,
                        albumId,
                        albumName,
                        uploaderName
                    )
                }

                "task_reminder" -> {
                    val taskTitle = data["taskTitle"] ?: return
                    val taskDescription = data["taskDescription"] ?: return

                    NotificationUtils.showTaskReminderNotification(
                        this,
                        taskTitle,
                        taskDescription
                    )
                }

                "event_reminder" -> {
                    val eventTitle = data["eventTitle"] ?: return
                    val eventTime = data["eventTime"] ?: return

                    NotificationUtils.showEventReminderNotification(
                        this,
                        eventTitle,
                        eventTime
                    )
                }

                "general" -> {
                    val title = data["title"] ?: "ShaadiSaathi"
                    val message = data["message"] ?: return

                    NotificationUtils.showGeneralNotification(
                        this,
                        title,
                        message
                    )
                }

                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                    // Handle unknown type as general notification
                    val title = data["title"] ?: "ShaadiSaathi"
                    val message = data["message"] ?: "You have a new notification"

                    NotificationUtils.showGeneralNotification(
                        this,
                        title,
                        message
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling data message", e)
        }
    }

    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        try {
            Log.d(TAG, "Notification Title: ${notification.title}")
            Log.d(TAG, "Notification Body: ${notification.body}")

            // Show general notification for notification payload
            val title = notification.title ?: "ShaadiSaathi"
            val body = notification.body ?: "You have a new notification"

            NotificationUtils.showGeneralNotification(
                this,
                title,
                body
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification message", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send token to your server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        try {
            // TODO: Implement sending token to your server
            // This is where you would make an API call to your backend
            // to store the FCM token for this user

            Log.d(TAG, "Token sent to server: $token")

            // Example implementation:
            // ApiService.updateFCMToken(token)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending token to server", e)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Some messages were deleted on the server")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Message sent successfully: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Error sending message: $msgId", exception)
    }
}
