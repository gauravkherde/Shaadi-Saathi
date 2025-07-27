package com.gaurav.shaadisaathi.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gaurav.shaadisaathi.models.Event
import kotlinx.coroutines.tasks.await

class EventRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "EventRepository"

    suspend fun addEvent(event: Event): Result<String> {
        return try {
            firestore.collection("events").document(event.id).set(event).await()
            Log.d(TAG, "Event added successfully: ${event.id}")
            Result.success(event.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event", e)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            firestore.collection("events").document(event.id).set(event).await()
            Log.d(TAG, "Event updated successfully: ${event.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId).delete().await()
            Log.d(TAG, "Event deleted successfully: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            Result.failure(e)
        }
    }

    suspend fun getEvent(eventId: String): Result<Event?> {
        return try {
            val document = firestore.collection("events").document(eventId).get().await()
            val event = document.toObject(Event::class.java)
            Log.d(TAG, "Event retrieved: $eventId")
            Result.success(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting event", e)
            Result.failure(e)
        }
    }

    suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("events")
                .whereEqualTo("hostId", currentUser.uid)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = documents.mapNotNull { it.toObject(Event::class.java) }
            Log.d(TAG, "Retrieved ${events.size} events")
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all events", e)
            Result.failure(e)
        }
    }

    suspend fun getUpcomingEvents(): Result<List<Event>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val currentTime = System.currentTimeMillis()

            val documents = firestore.collection("events")
                .whereEqualTo("hostId", currentUser.uid)
                .whereGreaterThan("date", currentTime)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = documents.mapNotNull { it.toObject(Event::class.java) }
            Log.d(TAG, "Retrieved ${events.size} upcoming events")
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting upcoming events", e)
            Result.failure(e)
        }
    }

    suspend fun getEventsByType(type: String): Result<List<Event>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("events")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("type", type)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = documents.mapNotNull { it.toObject(Event::class.java) }
            Log.d(TAG, "Retrieved ${events.size} events of type: $type")
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting events by type", e)
            Result.failure(e)
        }
    }
}
