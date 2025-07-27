package com.gaurav.shaadisaathi.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gaurav.shaadisaathi.models.Guest
import kotlinx.coroutines.tasks.await

class GuestRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "GuestRepository"

    suspend fun addGuest(guest: Guest): Result<String> {
        return try {
            firestore.collection("guests").document(guest.id).set(guest).await()
            Log.d(TAG, "Guest added successfully: ${guest.id}")
            Result.success(guest.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding guest", e)
            Result.failure(e)
        }
    }

    suspend fun updateGuest(guest: Guest): Result<Unit> {
        return try {
            firestore.collection("guests").document(guest.id).set(guest).await()
            Log.d(TAG, "Guest updated successfully: ${guest.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating guest", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGuest(guestId: String): Result<Unit> {
        return try {
            firestore.collection("guests").document(guestId).delete().await()
            Log.d(TAG, "Guest deleted successfully: $guestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting guest", e)
            Result.failure(e)
        }
    }

    suspend fun getGuest(guestId: String): Result<Guest?> {
        return try {
            val document = firestore.collection("guests").document(guestId).get().await()
            val guest = document.toObject(Guest::class.java)
            Log.d(TAG, "Guest retrieved: $guestId")
            Result.success(guest)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting guest", e)
            Result.failure(e)
        }
    }

    suspend fun getAllGuests(): Result<List<Guest>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("guests")
                .whereEqualTo("hostId", currentUser.uid)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val guests = documents.mapNotNull { it.toObject(Guest::class.java) }
            Log.d(TAG, "Retrieved ${guests.size} guests")
            Result.success(guests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all guests", e)
            Result.failure(e)
        }
    }

    suspend fun getGuestsByCategory(category: String): Result<List<Guest>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("guests")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("category", category)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val guests = documents.mapNotNull { it.toObject(Guest::class.java) }
            Log.d(TAG, "Retrieved ${guests.size} guests for category: $category")
            Result.success(guests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting guests by category", e)
            Result.failure(e)
        }
    }

    suspend fun getGuestsByRSVPStatus(status: String): Result<List<Guest>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("guests")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("rsvpStatus", status)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val guests = documents.mapNotNull { it.toObject(Guest::class.java) }
            Log.d(TAG, "Retrieved ${guests.size} guests with RSVP status: $status")
            Result.success(guests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting guests by RSVP status", e)
            Result.failure(e)
        }
    }

    suspend fun updateGuestRSVP(guestId: String, newStatus: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "rsvpStatus" to newStatus,
                "rsvpResponseAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("guests").document(guestId).update(updates).await()
            Log.d(TAG, "Guest RSVP updated: $guestId -> $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating guest RSVP", e)
            Result.failure(e)
        }
    }

    suspend fun bulkImportGuests(guests: List<Guest>): Result<Int> {
        return try {
            val batch = firestore.batch()

            guests.forEach { guest ->
                val docRef = firestore.collection("guests").document(guest.id)
                batch.set(docRef, guest)
            }

            batch.commit().await()
            Log.d(TAG, "Bulk imported ${guests.size} guests")
            Result.success(guests.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error bulk importing guests", e)
            Result.failure(e)
        }
    }

    suspend fun getGuestStatistics(): Result<GuestStatistics> {
        return try {
            val guests = getAllGuests().getOrThrow()

            val totalGuests = guests.size
            val confirmedGuests = guests.count { it.rsvpStatus == "confirmed" }
            val pendingGuests = guests.count { it.rsvpStatus == "pending" }
            val declinedGuests = guests.count { it.rsvpStatus == "declined" }
            val totalAttending = guests.filter { it.rsvpStatus == "confirmed" }.sumOf { it.getTotalGuests() }
            val vipGuests = guests.count { it.isVip }
            val guestsWithPlusOne = guests.count { it.hasPlusOne }

            val categoryBreakdown = guests.groupBy { it.category }.mapValues { it.value.size }
            val mealPreferenceBreakdown = guests.groupBy { it.mealPreference }.mapValues { it.value.size }

            val statistics = GuestStatistics(
                totalGuests = totalGuests,
                confirmedGuests = confirmedGuests,
                pendingGuests = pendingGuests,
                declinedGuests = declinedGuests,
                totalAttending = totalAttending,
                vipGuests = vipGuests,
                guestsWithPlusOne = guestsWithPlusOne,
                categoryBreakdown = categoryBreakdown,
                mealPreferenceBreakdown = mealPreferenceBreakdown
            )

            Log.d(TAG, "Guest statistics calculated")
            Result.success(statistics)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting guest statistics", e)
            Result.failure(e)
        }
    }
}

data class GuestStatistics(
    val totalGuests: Int,
    val confirmedGuests: Int,
    val pendingGuests: Int,
    val declinedGuests: Int,
    val totalAttending: Int,
    val vipGuests: Int,
    val guestsWithPlusOne: Int,
    val categoryBreakdown: Map<String, Int>,
    val mealPreferenceBreakdown: Map<String, Int>
)
