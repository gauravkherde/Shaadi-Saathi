package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "SplashActivity"
    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start fade-in animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.tvAppName.startAnimation(fadeIn)

        // Check user authentication status after delay
        binding.tvAppName.postDelayed({
            checkUserAuthenticationStatus()
        }, SPLASH_DELAY)
    }

    private fun checkUserAuthenticationStatus() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d(TAG, "User is logged in: ${currentUser.uid}")
            fetchUserRoleAndNavigate(currentUser.uid)
        } else {
            Log.d(TAG, "No user logged in, going to LoginActivity")
            navigateToLogin()
        }
    }

    private fun fetchUserRoleAndNavigate(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val name = document.getString("name")

                    Log.d(TAG, "User role: $role, name: $name")

                    when (role) {
                        "host" -> {
                            Log.d(TAG, "Navigating to HostDashboardActivity")
                            navigateToActivity(HostDashboardActivity::class.java)
                        }
                        "guest" -> {
                            Log.d(TAG, "Navigating to GuestMainActivity")
                            navigateToActivity(GuestMainActivity::class.java)
                        }
                        else -> {
                            Log.w(TAG, "Unknown or null role: $role")
                            navigateToLogin()
                        }
                    }
                } else {
                    Log.w(TAG, "User document does not exist")
                    // User document doesn't exist, sign out and go to login
                    auth.signOut()
                    navigateToLogin()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch user data: ${exception.message}")
                // On failure, go to login to be safe
                navigateToLogin()
            }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        navigateToActivity(LoginActivity::class.java)
    }

    override fun onBackPressed() {
        // Disable back button on splash screen
        // Do nothing
    }
}
