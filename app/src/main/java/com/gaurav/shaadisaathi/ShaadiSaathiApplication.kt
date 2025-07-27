package com.gaurav.shaadisaathi

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.utils.NotificationUtils

class ShaadiSaathiApplication : Application() {

    private val TAG = "ShaadiSaathiApp"

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "ShaadiSaathi Application starting...")

        // Initialize Firebase with error handling
        initializeFirebaseWithRetry()

        // Initialize notification channels
        initializeNotifications()

        Log.d(TAG, "ShaadiSaathi Application initialized")
    }

    private fun initializeFirebaseWithRetry() {
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase App initialized")

            // Test if database URL exists
            val database = FirebaseDatabase.getInstance()
            val databaseUrl = database.reference.toString()

            if (databaseUrl.contains("firebaseio.com")) {
                Log.d(TAG, "✅ Database URL found: $databaseUrl")

                // Test connection immediately
                testDatabaseConnectionImmediate()

                // Enable offline support
                try {
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                    Log.d(TAG, "✅ Database offline support enabled")
                } catch (e: Exception) {
                    Log.w(TAG, "Offline support already enabled or failed: ${e.message}")
                }

            } else {
                Log.e(TAG, "❌ No database URL found in configuration")
                Log.e(TAG, "Please enable Realtime Database in Firebase Console")
            }

            // Initialize other Firebase services
            FirebaseFirestore.getInstance()
            FirebaseAuth.getInstance()
            Log.d(TAG, "✅ All Firebase services initialized")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
        }
    }

    private fun testDatabaseConnectionImmediate() {
        val database = FirebaseDatabase.getInstance()
        val testRef = database.reference.child("connection_test")

        Log.d(TAG, "Testing database connection...")

        testRef.setValue("test_${System.currentTimeMillis()}")
            .addOnSuccessListener {
                Log.d(TAG, "✅ Database connection successful")
                testRef.removeValue() // Clean up
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Database connection failed: ${e.message}")

                when {
                    e.message?.contains("Permission denied") == true -> {
                        Log.e(TAG, "Fix: Update database rules in Firebase Console")
                    }
                    e.message?.contains("network") == true -> {
                        Log.e(TAG, "Fix: Check internet connection")
                    }
                    else -> {
                        Log.e(TAG, "Fix: Verify database URL in google-services.json")
                    }
                }
            }
    }

    private fun initializeNotifications() {
        try {
            NotificationUtils.createNotificationChannels(this)
            Log.d(TAG, "✅ Notification channels created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Notification initialization failed", e)
        }
    }

    /**
     * Public method to test database connectivity from activities
     */
    fun testDatabaseConnectivity(callback: (Boolean, String) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val testRef = database.reference.child("connectivity_test")

        testRef.setValue("test_${System.currentTimeMillis()}")
            .addOnSuccessListener {
                testRef.removeValue()
                callback(true, "Database connected successfully")
            }
            .addOnFailureListener { e ->
                val errorMsg = when {
                    e.message?.contains("Permission denied") == true ->
                        "Permission denied - Check Firebase rules"
                    e.message?.contains("network") == true ->
                        "Network error - Check internet connection"
                    else ->
                        "Database error: ${e.message}"
                }
                callback(false, errorMsg)
            }
    }
}
