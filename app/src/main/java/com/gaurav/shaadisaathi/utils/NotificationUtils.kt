package com.gaurav.shaadisaathi.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.activities.ChatActivity
import com.gaurav.shaadisaathi.activities.PhotoAlbumActivity
import com.gaurav.shaadisaathi.activities.MainActivity

object NotificationUtils {

    private const val CHAT_CHANNEL_ID = "chat_notifications"
    private const val PHOTO_CHANNEL_ID = "photo_notifications"
    private const val GENERAL_CHANNEL_ID = "general_notifications"

    private const val CHAT_NOTIFICATION_ID = 1001
    private const val PHOTO_NOTIFICATION_ID = 1002
    private const val GENERAL_NOTIFICATION_ID = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Chat notifications channel
            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                setShowBadge(true)
            }

            // Photo notifications channel
            val photoChannel = NotificationChannel(
                PHOTO_CHANNEL_ID,
                "Photo Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new photos and albums"
                enableVibration(true)
                setShowBadge(true)
            }

            // General notifications channel
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(photoChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    fun showChatNotification(
        context: Context,
        chatRoomId: String,
        chatRoomName: String,
        senderName: String,
        message: String
    ) {
        // Create intent to open chat activity
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("chatRoomId", chatRoomId)
            putExtra("chatRoomName", chatRoomName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            CHAT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(chatRoomName)
            .setContentText("$senderName: $message")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$senderName: $message")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup("chat_group")
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                chatRoomId.hashCode(), // Use unique ID for each chat room
                notification
            )
        } catch (e: SecurityException) {
            // Handle notification permission not granted
            android.util.Log.e("NotificationUtils", "Notification permission not granted", e)
        }
    }

    fun showPhotoNotification(
        context: Context,
        albumId: String,
        albumName: String,
        uploaderName: String
    ) {
        // Create intent to open photo album activity
        val intent = Intent(context, PhotoAlbumActivity::class.java).apply {
            putExtra("albumId", albumId)
            putExtra("albumName", albumName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            PHOTO_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PHOTO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_photo)
            .setContentTitle("New Photos Added")
            .setContentText("$uploaderName added new photos to $albumName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$uploaderName added new photos to $albumName album. Tap to view the latest updates!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup("photo_group")
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                albumId.hashCode(), // Use unique ID for each album
                notification
            )
        } catch (e: SecurityException) {
            // Handle notification permission not granted
            android.util.Log.e("NotificationUtils", "Notification permission not granted", e)
        }
    }

    fun showGeneralNotification(
        context: Context,
        title: String,
        message: String,
        targetActivity: Class<*> = MainActivity::class.java
    ) {
        val intent = Intent(context, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            GENERAL_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                GENERAL_NOTIFICATION_ID,
                notification
            )
        } catch (e: SecurityException) {
            // Handle notification permission not granted
            android.util.Log.e("NotificationUtils", "Notification permission not granted", e)
        }
    }

    fun showTaskReminderNotification(
        context: Context,
        taskTitle: String,
        taskDescription: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openFragment", "tasks")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1004,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_placeholder)
            .setContentTitle("Task Reminder: $taskTitle")
            .setContentText(taskDescription)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't forget: $taskDescription")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1004, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationUtils", "Notification permission not granted", e)
        }
    }

    fun showEventReminderNotification(
        context: Context,
        eventTitle: String,
        eventTime: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openFragment", "events")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1005,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_event)
            .setContentTitle("Event Reminder: $eventTitle")
            .setContentText("Scheduled for $eventTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your event '$eventTitle' is scheduled for $eventTime. Don't miss it!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1005, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationUtils", "Notification permission not granted", e)
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
