package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Creating LoginActivity")

            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = FirebaseAuth.getInstance()

            Log.d(TAG, "LoginActivity created successfully")

            binding.btnLogin.setOnClickListener {
                performLogin()
            }

            binding.tvGoToRegister.setOnClickListener {
                navigateToRegister()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing login: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Input validation
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return
        }

        // Disable login button and show loading state
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Signing in..."

        Log.d(TAG, "Attempting login for email: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase authentication successful")

                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        Log.d(TAG, "User UID: $uid")
                        fetchUserRoleAndNavigate(uid)
                    } else {
                        Log.e(TAG, "User UID is null after successful authentication")
                        handleLoginError("Authentication error: User ID not found")
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown authentication error"
                    Log.e(TAG, "Firebase authentication failed: $errorMessage", task.exception)
                    handleLoginError("Authentication failed: $errorMessage")
                }
            }
    }

    private fun fetchUserRoleAndNavigate(uid: String) {
        Log.d(TAG, "Fetching user role for UID: $uid")

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                try {
                    if (doc.exists()) {
                        val role = doc.getString("role")
                        val name = doc.getString("name")
                        val email = doc.getString("email")

                        Log.d(TAG, "User data - Name: $name, Role: $role, Email: $email")

                        when (role) {
                            "host" -> {
                                Log.d(TAG, "Navigating to HostDashboardActivity")
                                Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                                navigateToHostDashboard()
                            }
                            "guest" -> {
                                Log.d(TAG, "Navigating to GuestMainActivity")
                                Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                                navigateToGuestMain()
                            }
                            else -> {
                                Log.e(TAG, "Unknown or null user role: $role")
                                handleLoginError("Invalid user role: $role. Please contact support.")
                            }
                        }
                    } else {
                        Log.e(TAG, "User document does not exist for UID: $uid")
                        handleLoginError("User profile not found. Please register again.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing user data", e)
                    handleLoginError("Error processing user data: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch user data from Firestore", e)
                handleLoginError("Failed to fetch user profile: ${e.message}")
            }
    }

    private fun navigateToHostDashboard() {
        try {
            val intent = Intent(this, HostDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d(TAG, "Successfully navigated to HostDashboardActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to HostDashboardActivity", e)
            handleLoginError("Error loading host dashboard: ${e.message}")
        }
    }

    private fun navigateToGuestMain() {
        try {
            val intent = Intent(this, GuestMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d(TAG, "Successfully navigated to GuestMainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to GuestMainActivity", e)
            handleLoginError("Error loading guest dashboard: ${e.message}")
        }
    }

    private fun navigateToRegister() {
        try {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "Navigated to RegisterActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to RegisterActivity", e)
            Toast.makeText(this, "Error opening registration: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLoginError(message: String) {
        Log.e(TAG, "Login error: $message")

        // Reset UI state
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Login"

        // Show error to user
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "LoginActivity onStart")

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, checking role")
            fetchUserRoleAndNavigate(currentUser.uid)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "LoginActivity onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoginActivity onDestroy")
    }

    override fun onBackPressed() {
        // Exit app confirmation
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
                finishAffinity() // Close all activities and exit app
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
