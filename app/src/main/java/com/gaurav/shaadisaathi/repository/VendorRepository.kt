package com.gaurav.shaadisaathi.repository

import com.gaurav.shaadisaathi.models.Vendor
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VendorRepository {

    private val database = FirebaseDatabase.getInstance()
    private val vendorsRef = database.getReference("vendors")

    suspend fun getVendorById(vendorId: String): Result<Vendor?> {
        return suspendCancellableCoroutine { continuation ->
            vendorsRef.child(vendorId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val vendor = snapshot.getValue(Vendor::class.java)
                        continuation.resume(Result.success(vendor))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun getAllVendors(): Result<List<Vendor>> {
        return suspendCancellableCoroutine { continuation ->
            vendorsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val vendors = mutableListOf<Vendor>()
                        for (childSnapshot in snapshot.children) {
                            val vendor = childSnapshot.getValue(Vendor::class.java)
                            vendor?.let { vendors.add(it) }
                        }
                        continuation.resume(Result.success(vendors))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun addVendor(vendor: Vendor): Result<String> {
        return try {
            vendorsRef.child(vendor.id).setValue(vendor)
            Result.success(vendor.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendor(vendor: Vendor): Result<Boolean> {
        return try {
            vendorsRef.child(vendor.id).setValue(vendor)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVendor(vendorId: String): Result<Boolean> {
        return try {
            vendorsRef.child(vendorId).removeValue()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVendorsByCategory(category: String): Result<List<Vendor>> {
        return suspendCancellableCoroutine { continuation ->
            vendorsRef.orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val vendors = mutableListOf<Vendor>()
                            for (childSnapshot in snapshot.children) {
                                val vendor = childSnapshot.getValue(Vendor::class.java)
                                vendor?.let { vendors.add(it) }
                            }
                            continuation.resume(Result.success(vendors))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                })
        }
    }
}
