package com.gaurav.shaadisaathi.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gaurav.shaadisaathi.models.Vendor
import kotlinx.coroutines.tasks.await

class VendorRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "VendorRepository"

    suspend fun addVendor(vendor: Vendor): Result<String> {
        return try {
            firestore.collection("vendors").document(vendor.id).set(vendor).await()
            Log.d(TAG, "Vendor added successfully: ${vendor.id}")
            Result.success(vendor.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding vendor", e)
            Result.failure(e)
        }
    }

    suspend fun updateVendor(vendor: Vendor): Result<Unit> {
        return try {
            firestore.collection("vendors").document(vendor.id).set(vendor).await()
            Log.d(TAG, "Vendor updated successfully: ${vendor.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vendor", e)
            Result.failure(e)
        }
    }

    suspend fun deleteVendor(vendorId: String): Result<Unit> {
        return try {
            firestore.collection("vendors").document(vendorId).delete().await()
            Log.d(TAG, "Vendor deleted successfully: $vendorId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting vendor", e)
            Result.failure(e)
        }
    }

    suspend fun getVendor(vendorId: String): Result<Vendor?> {
        return try {
            val document = firestore.collection("vendors").document(vendorId).get().await()
            val vendor = document.toObject(Vendor::class.java)
            Log.d(TAG, "Vendor retrieved: $vendorId")
            Result.success(vendor)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vendor", e)
            Result.failure(e)
        }
    }

    suspend fun getAllVendors(): Result<List<Vendor>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("vendors")
                .whereEqualTo("hostId", currentUser.uid)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val vendors = documents.mapNotNull { it.toObject(Vendor::class.java) }
            Log.d(TAG, "Retrieved ${vendors.size} vendors")
            Result.success(vendors)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all vendors", e)
            Result.failure(e)
        }
    }

    suspend fun getVendorsByCategory(category: String): Result<List<Vendor>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("vendors")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("category", category)
                .orderBy("rating", Query.Direction.DESCENDING)
                .get()
                .await()

            val vendors = documents.mapNotNull { it.toObject(Vendor::class.java) }
            Log.d(TAG, "Retrieved ${vendors.size} vendors for category: $category")
            Result.success(vendors)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vendors by category", e)
            Result.failure(e)
        }
    }

    suspend fun getFavoriteVendors(): Result<List<Vendor>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("vendors")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("isFavorite", true)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val vendors = documents.mapNotNull { it.toObject(Vendor::class.java) }
            Log.d(TAG, "Retrieved ${vendors.size} favorite vendors")
            Result.success(vendors)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorite vendors", e)
            Result.failure(e)
        }
    }

    suspend fun getBookedVendors(): Result<List<Vendor>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val documents = firestore.collection("vendors")
                .whereEqualTo("hostId", currentUser.uid)
                .whereEqualTo("isBooked", true)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            val vendors = documents.mapNotNull { it.toObject(Vendor::class.java) }
            Log.d(TAG, "Retrieved ${vendors.size} booked vendors")
            Result.success(vendors)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booked vendors", e)
            Result.failure(e)
        }
    }
}
