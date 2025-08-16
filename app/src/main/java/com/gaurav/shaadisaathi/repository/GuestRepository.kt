package com.gaurav.shaadisaathi.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gaurav.shaadisaathi.models.Guest
import kotlinx.coroutines.tasks.await

class GuestRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addGuest(guest: Guest): Result<String> {
        return try {
            firestore.collection("guests").document(guest.id).set(guest).await()
            Result.success(guest.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllGuests(): Result<List<Guest>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            val snapshot = firestore.collection("guests")
                .whereEqualTo("hostId", currentUser.uid)
                .orderBy("name")
                .get()
                .await()

            val guests = snapshot.toObjects(Guest::class.java)
            Result.success(guests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuest(guestId: String): Result<Guest?> {
        return try {
            val snapshot = firestore.collection("guests").document(guestId).get().await()
            val guest = snapshot.toObject(Guest::class.java)
            Result.success(guest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGuest(guest: Guest): Result<Unit> {
        return try {
            firestore.collection("guests").document(guest.id).set(guest).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGuest(guestId: String): Result<Unit> {
        return try {
            firestore.collection("guests").document(guestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuestStatistics(): Result<GuestStatistics> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            val snapshot = firestore.collection("guests")
                .whereEqualTo("hostId", currentUser.uid)
                .get()
                .await()

            val guests = snapshot.toObjects(Guest::class.java)
            val stats = calculateStatistics(guests)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStatistics(guests: List<Guest>): GuestStatistics {
        val totalGuests = guests.size
        val confirmedGuests = guests.count { it.rsvpStatus == "confirmed" }
        val pendingGuests = guests.count { it.rsvpStatus == "pending" }
        val declinedGuests = guests.count { it.rsvpStatus == "declined" }
        val totalAttending = guests.sumOf { if (it.rsvpStatus == "confirmed") it.getTotalGuests() else 0 }

        val categoryBreakdown = guests.groupBy { it.category }.mapValues { it.value.size }
        val mealPreferenceBreakdown = guests.groupBy { it.mealPreference }.mapValues { it.value.size }

        return GuestStatistics(
            totalGuests = totalGuests,
            confirmedGuests = confirmedGuests,
            pendingGuests = pendingGuests,
            declinedGuests = declinedGuests,
            totalAttending = totalAttending,
            categoryBreakdown = categoryBreakdown,
            mealPreferenceBreakdown = mealPreferenceBreakdown
        )
    }
}

data class GuestStatistics(
    val totalGuests: Int,
    val confirmedGuests: Int,
    val pendingGuests: Int,
    val declinedGuests: Int,
    val totalAttending: Int,
    val categoryBreakdown: Map<String, Int>,
    val mealPreferenceBreakdown: Map<String, Int>
)
