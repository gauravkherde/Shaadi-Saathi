package com.gaurav.shaadisaathi.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.activities.GuestListActivity

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    private const val CHANNEL_ID = "wedding_reminders"
    private const val CHANNEL_NAME = "Wedding Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for wedding events and RSVP reminders"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Notification channel created")
        }
    }

    fun scheduleRSVPReminder(
        context: Context,
        guestName: String,
        eventName: String,
        reminderTime: Long,
        requestCode: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "rsvp_reminder")
            putExtra("guest_name", guestName)
            putExtra("event_name", eventName)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            Log.d(TAG, "RSVP reminder scheduled for $guestName")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling RSVP reminder", e)
        }
    }

    fun scheduleEventReminder(
        context: Context,
        eventName: String,
        eventTime: Long,
        requestCode: Int,
        reminderMinutesBefore: Int = 1440 // 24 hours
    ) {
        val reminderTime = eventTime - (reminderMinutesBefore * 60 * 1000)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "event_reminder")
            putExtra("event_name", eventName)
            putExtra("event_time", eventTime)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            Log.d(TAG, "Event reminder scheduled for $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling event reminder", e)
        }
    }

    fun cancelReminder(context: Context, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Log.d(TAG, "Reminder cancelled: $requestCode")
    }

    fun showInstantNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, GuestListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }

        Log.d(TAG, "Instant notification shown: $title")
    }
}

class ReminderReceiver : BroadcastReceiver() {

    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")

        when (type) {
            "rsvp_reminder" -> {
                val guestName = intent.getStringExtra("guest_name") ?: ""
                val eventName = intent.getStringExtra("event_name") ?: ""

                NotificationScheduler.showInstantNotification(
                    context,
                    "RSVP Reminder",
                    "Reminder: $guestName hasn't responded to $eventName invitation"
                )
            }
            "event_reminder" -> {
                val eventName = intent.getStringExtra("event_name") ?: ""

                NotificationScheduler.showInstantNotification(
                    context,
                    "Event Reminder",
                    "$eventName is tomorrow! Make sure everything is ready."
                )
            }
        }

        Log.d(TAG, "Reminder received and processed: $type")
    }
}
