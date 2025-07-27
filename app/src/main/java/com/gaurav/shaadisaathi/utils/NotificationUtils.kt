package com.gaurav.shaadisaathi.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.activities.ChatActivity
import com.gaurav.shaadisaathi.activities.PhotoGalleryActivity

object NotificationUtils {

    private const val CHANNEL_ID_CHAT = "chat_notifications"
    private const val CHANNEL_ID_PHOTO = "photo_notifications"
    private const val CHANNEL_ID_EVENT = "event_notifications"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Chat notifications channel
            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
            }

            // Photo notifications channel
            val photoChannel = NotificationChannel(
                CHANNEL_ID_PHOTO,
                "Photo Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new photos"
            }

            // Event notifications channel
            val eventChannel = NotificationChannel(
                CHANNEL_ID_EVENT,
                "Event Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for event updates"
            }

            notificationManager.createNotificationChannels(listOf(chatChannel, photoChannel, eventChannel))
        }
    }

    fun showChatNotification(
        context: Context,
        chatRoomId: String,
        chatRoomName: String,
        senderName: String,
        message: String
    ) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("chatRoomId", chatRoomId)
            putExtra("chatRoomName", chatRoomName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(chatRoomName)
            .setContentText("$senderName: $message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(chatRoomId.hashCode(), notification)
    }

    fun showPhotoNotification(
        context: Context,
        albumId: String,
        albumName: String,
        uploaderName: String
    ) {
        val intent = Intent(context, PhotoGalleryActivity::class.java).apply {
            putExtra("albumId", albumId)
            putExtra("albumName", albumName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PHOTO)
            .setSmallIcon(R.drawable.ic_photo)
            .setContentTitle("New Photo Added")
            .setContentText("$uploaderName added a new photo to $albumName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(albumId.hashCode(), notification)
    }
}
