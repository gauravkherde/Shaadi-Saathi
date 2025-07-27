package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val role = when {
            binding.rgRole.checkedRadioButtonId == binding.rbHost.id -> "host"
            binding.rgRole.checkedRadioButtonId == binding.rbGuest.id -> "guest"
            else -> ""
        }

        // Validation
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            binding.etName.requestFocus()
            return
        }

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

        if (role.isEmpty()) {
            Toast.makeText(this, "Please select your role", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show loading
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Creating account..."

        // Create Firebase user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        saveUserDataToFirestore(uid, name, email, role)
                    } else {
                        Log.e(TAG, "User UID is null after successful registration")
                        showRegistrationError("Registration failed. Please try again.")
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    Log.e(TAG, "Firebase Auth error: $errorMessage")
                    showRegistrationError("Registration failed: $errorMessage")
                }
            }
    }

    private fun saveUserDataToFirestore(uid: String, name: String, email: String, role: String) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User data saved successfully for role: $role")

                // Reset button state
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"

                Toast.makeText(this, "Registration successful! Welcome $name", Toast.LENGTH_SHORT).show()

                // Navigate based on role with proper intent flags
                navigateToMainScreen(role)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error: ${e.message}")
                showRegistrationError("Failed to save user data: ${e.message}")
            }
    }

    private fun navigateToMainScreen(role: String) {
        // Add delay to ensure UI updates complete
        binding.root.postDelayed({
            val intent = when (role) {
                "host" -> Intent(this@RegisterActivity, HostDashboardActivity::class.java)
                "guest" -> Intent(this@RegisterActivity, GuestMainActivity::class.java)
                else -> {
                    Log.e(TAG, "Unknown role: $role")
                    Intent(this@RegisterActivity, LoginActivity::class.java)
                }
            }

            // Clear the activity stack and start fresh
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 800) // 800ms delay to show success message
    }

    private fun showRegistrationError(message: String) {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Register"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
