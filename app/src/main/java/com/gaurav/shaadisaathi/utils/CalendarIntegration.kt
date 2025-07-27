package com.gaurav.shaadisaathi.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import java.util.*

object CalendarIntegration {

    private const val TAG = "CalendarIntegration"

    fun addEventToCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        startTime: Long,
        endTime: Long,
        allDay: Boolean = false
    ): Result<Long> {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                putExtra(CalendarContract.Events.ALL_DAY, allDay)
                putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
            }

            context.startActivity(intent)
            Log.d(TAG, "Calendar event created: $title")
            Result.success(0L) // Return 0 as we're using intent

        } catch (e: Exception) {
            Log.e(TAG, "Error adding event to calendar", e)
            Result.failure(e)
        }
    }

    fun createWeddingReminder(
        context: Context,
        eventName: String,
        eventDate: Long,
        reminderMinutes: Int = 1440 // 24 hours before
    ) {
        val reminderTime = eventDate - (reminderMinutes * 60 * 1000)

        addEventToCalendar(
            context = context,
            title = "Reminder: $eventName",
            description = "Wedding event reminder - $eventName is tomorrow!",
            location = "",
            startTime = reminderTime,
            endTime = reminderTime + (30 * 60 * 1000), // 30 minutes duration
            allDay = false
        )
    }

    fun addMultipleWeddingEvents(
        context: Context,
        events: List<WeddingEvent>
    ): Result<List<Long>> {
        return try {
            val eventIds = mutableListOf<Long>()

            events.forEach { event ->
                val result = addEventToCalendar(
                    context = context,
                    title = event.title,
                    description = event.description,
                    location = event.location,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    allDay = event.allDay
                )

                if (result.isSuccess) {
                    eventIds.add(result.getOrDefault(0L))
                }
            }

            Log.d(TAG, "Multiple events added to calendar: ${eventIds.size}")
            Result.success(eventIds)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding multiple events to calendar", e)
            Result.failure(e)
        }
    }

    data class WeddingEvent(
        val title: String,
        val description: String,
        val location: String,
        val startTime: Long,
        val endTime: Long,
        val allDay: Boolean = false
    )

    fun createWeddingTimeline(): List<WeddingEvent> {
        val calendar = Calendar.getInstance()
        val baseDate = calendar.timeInMillis

        return listOf(
            WeddingEvent(
                title = "Mehendi Ceremony",
                description = "Traditional henna ceremony with family and friends",
                location = "Home",
                startTime = baseDate - (2 * 24 * 60 * 60 * 1000), // 2 days before
                endTime = baseDate - (2 * 24 * 60 * 60 * 1000) + (4 * 60 * 60 * 1000), // 4 hours
                allDay = false
            ),
            WeddingEvent(
                title = "Sangam Ceremony",
                description = "Pre-wedding ceremony with both families",
                location = "Wedding Venue",
                startTime = baseDate - (24 * 60 * 60 * 1000), // 1 day before
                endTime = baseDate - (24 * 60 * 60 * 1000) + (3 * 60 * 60 * 1000), // 3 hours
                allDay = false
            ),
            WeddingEvent(
                title = "Wedding Ceremony",
                description = "Main wedding ceremony",
                location = "Wedding Venue",
                startTime = baseDate,
                endTime = baseDate + (6 * 60 * 60 * 1000), // 6 hours
                allDay = false
            ),
            WeddingEvent(
                title = "Reception",
                description = "Wedding reception dinner and celebration",
                location = "Reception Venue",
                startTime = baseDate + (24 * 60 * 60 * 1000), // 1 day after
                endTime = baseDate + (24 * 60 * 60 * 1000) + (4 * 60 * 60 * 1000), // 4 hours
                allDay = false
            )
        )
    }
}
