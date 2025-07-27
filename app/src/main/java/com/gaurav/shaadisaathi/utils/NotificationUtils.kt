package com.gaurav.shaadisaathi.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.gaurav.shaadisaathi.R

object NotificationUtils {

    private const val CHANNEL_GENERAL = "general"
    private const val CHANNEL_RSVP = "rsvp_reminders"
    private const val CHANNEL_EVENTS = "event_reminders"
    private const val CHANNEL_PHOTOS = "photo_uploads"
    private const val CHANNEL_CHAT = "chat_messages"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General notifications
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            // RSVP reminders
            val rsvpChannel = NotificationChannel(
                CHANNEL_RSVP,
                "RSVP Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for pending RSVP responses"
            }

            // Event reminders
            val eventChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming wedding events"
            }

            // Photo uploads
            val photoChannel = NotificationChannel(
                CHANNEL_PHOTOS,
                "Photo Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for photo upload status"
            }

            // Chat messages
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New chat messages from wedding groups"
            }

            notificationManager.createNotificationChannels(listOf(
                generalChannel,
                rsvpChannel,
                eventChannel,
                photoChannel,
                chatChannel
            ))
        }
    }

    fun getChannelId(type: String): String {
        return when (type) {
            "rsvp" -> CHANNEL_RSVP
            "event" -> CHANNEL_EVENTS
            "photo" -> CHANNEL_PHOTOS
            "chat" -> CHANNEL_CHAT
            else -> CHANNEL_GENERAL
        }
    }
}
