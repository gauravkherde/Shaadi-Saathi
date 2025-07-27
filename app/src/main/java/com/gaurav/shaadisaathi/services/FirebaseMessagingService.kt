package com.gaurav.shaadisaathi.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gaurav.shaadisaathi.utils.NotificationUtils

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"

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
        }
    }

    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        // Handle notification payload if needed
        Log.d(TAG, "Notification Title: ${notification.title}")
        Log.d(TAG, "Notification Body: ${notification.body}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // TODO: Send token to your server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement sending token to your server
        Log.d(TAG, "Token sent to server: $token")
    }
}
