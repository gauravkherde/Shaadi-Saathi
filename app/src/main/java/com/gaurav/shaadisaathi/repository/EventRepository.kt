package com.gaurav.shaadisaathi.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.models.Event
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addEvent(event: Event): Result<String> {
        return try {
            firestore.collection("events").document(event.id).set(event).await()
            Result.success(event.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            val snapshot = firestore.collection("events")
                .whereEqualTo("hostId", currentUser.uid)
                .orderBy("date")
                .get()
                .await()

            val events = snapshot.toObjects(Event::class.java)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEvent(eventId: String): Result<Event?> {
        return try {
            val snapshot = firestore.collection("events").document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            firestore.collection("events").document(event.id).set(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
