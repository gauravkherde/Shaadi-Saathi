package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.databinding.ActivityCreateChatRoomBinding
import com.gaurav.shaadisaathi.models.ChatRoom

class CreateChatRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateChatRoomBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "CreateChatRoomActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupChatTypeSpinner()
        setupClickListeners()

        // Debug authentication
        debugAuthentication()

        Log.d(TAG, "CreateChatRoomActivity initialized")
    }

    private fun debugAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "✅ User authenticated: ${currentUser.uid}")
            Log.d(TAG, "User email: ${currentUser.email}")
        } else {
            Log.e(TAG, "❌ User NOT authenticated!")
            Toast.makeText(this, "Authentication required. Please login again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCreateChat.setOnClickListener {
            createChatRoom()
        }

        // Add step-by-step test button
        binding.btnTestStepByStep.setOnClickListener {
            testProductionRulesStepByStep()
        }

        // Add debug button for testing database rules
        binding.btnDebugRules.setOnClickListener {
            testDatabaseRulesDebug()
        }
    }

    private fun setupChatTypeSpinner() {
        val chatTypes = arrayOf("Event", "Family", "Announcement", "General")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chatTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerChatType.adapter = adapter
    }

    private fun createChatRoom() {
        val name = binding.etChatName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val type = binding.spinnerChatType.selectedItem.toString().lowercase()

        // Validation
        if (name.isEmpty()) {
            binding.etChatName.error = "Chat name is required"
            binding.etChatName.requestFocus()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state
        binding.btnCreateChat.isEnabled = false
        binding.btnCreateChat.text = "Creating..."
        binding.progressBar.visibility = View.VISIBLE

        Log.d(TAG, "Creating chat room: $name, type: $type")
        Log.d(TAG, "Current user: ${currentUser.uid}")

        // ADDED: Force refresh authentication token for production rules
        currentUser.getIdToken(true)
            .addOnSuccessListener { result ->
                Log.d(TAG, "✅ Authentication token refreshed")
                proceedWithValidatedUser(name, description, type, currentUser)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Authentication token refresh failed", e)
                handleError("Authentication failed. Please login again.")
            }
    }

    private fun proceedWithValidatedUser(name: String, description: String, type: String, user: FirebaseUser) {
        Log.d(TAG, "Proceeding with validated user: ${user.uid}")

        // Test database connection first
        testDatabaseConnection { isConnected ->
            if (isConnected) {
                // Verify user role and proceed
                verifyUserAndCreateRoom(name, description, type, user.uid)
            } else {
                handleError("Cannot connect to database. Check internet connection.")
            }
        }
    }

    private fun testDatabaseConnection(callback: (Boolean) -> Unit) {
        Log.d(TAG, "Testing database connection...")

        val testRef = database.reference.child("connection_test")
        testRef.setValue("test_${System.currentTimeMillis()}")
            .addOnSuccessListener {
                Log.d(TAG, "✅ Database connection successful")
                testRef.removeValue() // Clean up
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Database connection failed", e)
                Toast.makeText(this, "Database connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                callback(false)
            }
    }

    private fun verifyUserAndCreateRoom(name: String, description: String, type: String, userId: String) {
        Log.d(TAG, "Verifying user role for: $userId")

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val userRole = userDoc.getString("role") ?: "guest"
                    val userName = userDoc.getString("name") ?: "User"

                    Log.d(TAG, "User role: $userRole, name: $userName")

                    // Allow both hosts and guests to create chat rooms
                    proceedWithChatRoomCreation(name, description, type, userId, userName)
                } else {
                    Log.e(TAG, "User document not found")
                    handleError("User profile not found. Please complete registration.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error verifying user role", e)
                // Continue anyway with default values
                proceedWithChatRoomCreation(name, description, type, userId, "User")
            }
    }

    private fun proceedWithChatRoomCreation(
        name: String,
        description: String,
        type: String,
        hostId: String,
        hostName: String
    ) {
        Log.d(TAG, "Proceeding with chat room creation...")

        // Generate unique chat room ID using push()
        val chatRoomRef = database.reference.child("chatRooms").push()
        val chatRoomId = chatRoomRef.key

        if (chatRoomId == null) {
            handleError("Error generating chat room ID")
            return
        }

        Log.d(TAG, "Generated chat room ID: $chatRoomId")

        // Create member list (start with creator)
        val membersList = mutableListOf<String>()
        membersList.add(hostId)

        // FIXED: Ensure all required fields are properly set
        val chatRoom = ChatRoom(
            id = chatRoomId,
            name = name,
            description = description,
            type = type,
            eventId = "",
            hostId = hostId, // CRITICAL: Make sure this matches current user
            members = membersList,
            lastMessage = "Chat room created by $hostName",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageSender = hostName,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        // Debug before saving
        debugProductionRulesFailure(chatRoom, chatRoomId)

        // Save to Firebase Realtime Database with error handling
        saveChatRoomToDatabase(chatRoomId, chatRoom)
    }

    private fun debugProductionRulesFailure(chatRoom: ChatRoom, chatRoomId: String) {
        val currentUser = auth.currentUser

        Log.d(TAG, "=== DEBUGGING PRODUCTION RULES ===")
        Log.d(TAG, "Current User ID: ${currentUser?.uid}")
        Log.d(TAG, "Chat Room Host ID: ${chatRoom.hostId}")
        Log.d(TAG, "Are they equal? ${currentUser?.uid == chatRoom.hostId}")
        Log.d(TAG, "Chat Room Data: $chatRoom")
        Log.d(TAG, "Chat Room ID: $chatRoomId")

        // Test if user is authenticated
        if (currentUser != null) {
            Log.d(TAG, "✅ User is authenticated")
            Log.d(TAG, "User email: ${currentUser.email}")
        } else {
            Log.e(TAG, "❌ User is NOT authenticated")
        }
    }

    private fun saveChatRoomToDatabase(chatRoomId: String, chatRoom: ChatRoom) {
        Log.d(TAG, "Saving chat room to database: $chatRoomId")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            handleError("User authentication lost. Please login again.")
            return
        }

        // Verify the hostId matches current user
        if (chatRoom.hostId != currentUser.uid) {
            Log.e(TAG, "Host ID mismatch: ${chatRoom.hostId} vs ${currentUser.uid}")
            handleError("Authentication mismatch. Please try again.")
            return
        }

        // ENHANCED: Debug the exact data being sent vs rules expectations
        debugProductionRulesData(chatRoomId, chatRoom, currentUser)

        val chatRoomRef = database.reference.child("chatRooms").child(chatRoomId)

        // CRITICAL: Use the exact data structure that production rules expect
        val chatRoomMap = mapOf(
            "id" to chatRoom.id,
            "name" to chatRoom.name,
            "description" to chatRoom.description,
            "type" to chatRoom.type,
            "eventId" to chatRoom.eventId,
            "hostId" to chatRoom.hostId, // CRITICAL for production rules
            "members" to chatRoom.members,
            "lastMessage" to chatRoom.lastMessage,
            "lastMessageTime" to chatRoom.lastMessageTime,
            "lastMessageSender" to chatRoom.lastMessageSender,
            "createdAt" to chatRoom.createdAt,
            "isActive" to chatRoom.isActive
        )

        Log.d(TAG, "Final data being saved: $chatRoomMap")
        Log.d(TAG, "Auth user: ${currentUser.uid}")
        Log.d(TAG, "Data hostId: ${chatRoomMap["hostId"]}")
        Log.d(TAG, "Match check: ${currentUser.uid == chatRoomMap["hostId"]}")

        // Try to save with detailed error handling
        chatRoomRef.setValue(chatRoomMap)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Chat room created successfully with production rules")

                binding.progressBar.visibility = View.GONE
                binding.btnCreateChat.isEnabled = true
                binding.btnCreateChat.text = "Create Chat Room"

                Toast.makeText(this, "Chat room '${chatRoom.name}' created successfully!", Toast.LENGTH_SHORT).show()

                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Production rules failed during actual creation", e)

                // Detailed analysis of the failure
                analyzeProductionRuleFailure(e, currentUser, chatRoomMap)
            }
    }

    private fun debugProductionRulesData(chatRoomId: String, chatRoom: ChatRoom, currentUser: FirebaseUser) {
        Log.d(TAG, "=== PRODUCTION RULES DATA DEBUG ===")
        Log.d(TAG, "Chat Room ID: $chatRoomId")
        Log.d(TAG, "Current User: ${currentUser.uid}")
        Log.d(TAG, "Chat Room Host ID: ${chatRoom.hostId}")
        Log.d(TAG, "Members List: ${chatRoom.members}")
        Log.d(TAG, "User in Members: ${chatRoom.members.contains(currentUser.uid)}")
        Log.d(TAG, "Auth Token Valid: ${currentUser.isEmailVerified}")
        Log.d(TAG, "=== END DEBUG ===")
    }

    private fun analyzeProductionRuleFailure(error: Exception, currentUser: FirebaseUser, chatRoomData: Map<String, Any>) {
        Log.e(TAG, "=== ANALYZING PRODUCTION RULE FAILURE ===")
        Log.e(TAG, "Error message: ${error.message}")
        Log.e(TAG, "User ID: ${currentUser.uid}")
        Log.e(TAG, "Data hostId: ${chatRoomData["hostId"]}")
        Log.e(TAG, "IDs match: ${currentUser.uid == chatRoomData["hostId"]}")

        when {
            error.message?.contains("Permission denied") == true -> {
                // Show specific production rules error
                showProductionRulesDetailedError(currentUser, chatRoomData)
            }
            error.message?.contains("network") == true -> {
                handleError("Network error. Please check your internet connection.")
            }
            else -> {
                handleError("Production rules error: ${error.message}")
            }
        }
    }

    private fun showProductionRulesDetailedError(currentUser: FirebaseUser, chatRoomData: Map<String, Any>) {
        val errorDetails = StringBuilder()
        errorDetails.append("Production Firebase Rules Error\n\n")
        errorDetails.append("Debug Information:\n")
        errorDetails.append("• User ID: ${currentUser.uid}\n")
        errorDetails.append("• Host ID in data: ${chatRoomData["hostId"]}\n")
        errorDetails.append("• IDs match: ${currentUser.uid == chatRoomData["hostId"]}\n")
        errorDetails.append("• Auth token: ${if (currentUser.isEmailVerified) "Verified" else "Not verified"}\n\n")
        errorDetails.append("The production rules are stricter and check:\n")
        errorDetails.append("1. User authentication\n")
        errorDetails.append("2. hostId matches current user\n")
        errorDetails.append("3. Data structure compliance\n\n")
        errorDetails.append("Try using intermediate rules temporarily.")

        AlertDialog.Builder(this)
            .setTitle("Production Rules Failed")
            .setMessage(errorDetails.toString())
            .setPositiveButton("Use Intermediate Rules") { _, _ ->
                showIntermediateRulesInstructions()
            }
            .setNegativeButton("Retry") { _, _ ->
                binding.progressBar.visibility = View.GONE
                binding.btnCreateChat.isEnabled = true
                binding.btnCreateChat.text = "Create Chat Room"
            }
            .show()
    }

    private fun showIntermediateRulesInstructions() {
        val instructions = """
            Please update Firebase Rules to intermediate version:
            
            Go to Firebase Console → Realtime Database → Rules
            
            Replace with:
            {
              "rules": {
                "chatRooms": {
                  ".read": "auth != null",
                  ".write": "auth != null"
                },
                "messages": {
                  ".read": "auth != null",
                  ".write": "auth != null"  
                }
              }
            }
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Use Intermediate Rules")
            .setMessage(instructions)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun testProductionRulesStepByStep() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No user for testing", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "=== STEP-BY-STEP PRODUCTION RULES TEST ===")

        // Step 1: Test basic write
        val testRef = database.reference.child("chatRooms").child("step_test_1")
        val basicData = mapOf("hostId" to currentUser.uid)

        testRef.setValue(basicData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Step 1 PASSED: Basic write with hostId")

                // Step 2: Test complete data structure
                val completeData = mapOf(
                    "id" to "step_test_2",
                    "hostId" to currentUser.uid,
                    "name" to "Test Room",
                    "members" to listOf(currentUser.uid),
                    "createdAt" to System.currentTimeMillis()
                )

                database.reference.child("chatRooms").child("step_test_2").setValue(completeData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Step 2 PASSED: Complete data structure")
                        Toast.makeText(this, "All production rule tests PASSED", Toast.LENGTH_SHORT).show()

                        // Clean up
                        testRef.removeValue()
                        database.reference.child("chatRooms").child("step_test_2").removeValue()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Step 2 FAILED: Complete data - ${e.message}")
                        Toast.makeText(this, "Step 2 failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Step 1 FAILED: Basic write - ${e.message}")
                Toast.makeText(this, "Step 1 failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun testDatabaseRulesDebug() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No user authenticated for testing", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "=== TESTING DATABASE RULES ===")

        val testData = mapOf(
            "id" to "test_${System.currentTimeMillis()}",
            "hostId" to currentUser.uid,
            "name" to "Test Room",
            "members" to listOf(currentUser.uid),
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("chatRooms").child("debug_test").setValue(testData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Debug test write successful - rules are working")
                Toast.makeText(this, "Database rules test PASSED", Toast.LENGTH_SHORT).show()
                // Clean up
                database.reference.child("chatRooms").child("debug_test").removeValue()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Debug test write failed - rules issue: ${e.message}")
                Toast.makeText(this, "Database rules test FAILED: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleError(message: String) {
        Log.e(TAG, "Error: $message")

        binding.progressBar.visibility = View.GONE
        binding.btnCreateChat.isEnabled = true
        binding.btnCreateChat.text = "Create Chat Room"

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
