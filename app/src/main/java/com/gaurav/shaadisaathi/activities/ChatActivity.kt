package com.gaurav.shaadisaathi.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.gaurav.shaadisaathi.adapters.ChatMessageAdapter
import com.gaurav.shaadisaathi.databinding.ActivityChatBinding
import com.gaurav.shaadisaathi.models.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var messageAdapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private var chatRoomId = ""
    private var chatRoomName = ""
    private var currentUserName = ""
    private val TAG = "ChatActivity"

    // Permission constants
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                uploadImageMessage(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageMessage(it) }
    }

    private var currentPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        chatRoomId = intent.getStringExtra("chatRoomId") ?: ""
        chatRoomName = intent.getStringExtra("chatRoomName") ?: "Chat"

        binding.tvChatTitle.text = chatRoomName

        getCurrentUserName()
        setupRecyclerView()
        loadMessages()
        setupClickListeners()

        Log.d(TAG, "ChatActivity created for room: $chatRoomName")
    }

    private fun getCurrentUserName() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "User"
                Log.d(TAG, "Current user name: $currentUserName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user name", e)
                currentUserName = "User"
            }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            sendTextMessage()
        }

        binding.btnAttach.setOnClickListener {
            showAttachmentOptions()
        }

        // Optional: Send message on Enter key
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendTextMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter(
            messages = messageList,
            currentUserId = auth.currentUser?.uid ?: "",
            onImageClick = { imageUrl ->
                openImageViewer(imageUrl)
            },
            onVoicePlay = { message ->
                playVoiceMessage(message)
            }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // Start from bottom
            }
            adapter = messageAdapter
        }
    }

    private fun loadMessages() {
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "Chat room ID is empty")
            Toast.makeText(this, "Error: Invalid chat room", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Loading messages for chat room: $chatRoomId")

        database.reference.child("messages").child(chatRoomId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(ChatMessage::class.java)
                    message?.let {
                        Log.d(TAG, "New message received: ${it.message}")
                        messageList.add(it)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        binding.recyclerViewMessages.scrollToPosition(messageList.size - 1)

                        // Mark message as read if it's not from current user
                        if (it.senderId != auth.currentUser?.uid) {
                            markMessageAsRead(it)
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(ChatMessage::class.java)
                    message?.let { updatedMessage ->
                        val index = messageList.indexOfFirst { it.id == updatedMessage.id }
                        if (index != -1) {
                            messageList[index] = updatedMessage
                            messageAdapter.notifyItemChanged(index)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(ChatMessage::class.java)
                    message?.let { removedMessage ->
                        val index = messageList.indexOfFirst { it.id == removedMessage.id }
                        if (index != -1) {
                            messageList.removeAt(index)
                            messageAdapter.notifyItemRemoved(index)
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading messages", error.toException())
                    Toast.makeText(this@ChatActivity, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendTextMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val currentUser = auth.currentUser ?: return

        val messageId = database.reference.child("messages").child(chatRoomId).push().key ?: return

        val message = ChatMessage(
            id = messageId,
            chatRoomId = chatRoomId,
            senderId = currentUser.uid,
            senderName = currentUserName,
            message = messageText,
            messageType = "text",
            timestamp = System.currentTimeMillis()
        )

        Log.d(TAG, "Sending text message: $messageText")

        database.reference.child("messages").child(chatRoomId).child(messageId).setValue(message)
            .addOnSuccessListener {
                binding.etMessage.setText("")
                updateLastMessage(messageText)
                Log.d(TAG, "Text message sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending message", e)
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Photo", "Camera", "Voice Note")

        AlertDialog.Builder(this)
            .setTitle("Attach")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkStoragePermissionAndOpenGallery()
                    1 -> checkCameraPermissionAndTakePhoto()
                    2 -> recordVoiceNote()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (allPermissionsGranted()) {
            takePhoto()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, CAMERA_PERMISSION_CODE)
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun takePhoto() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            cameraLauncher.launch(currentPhotoUri)
            Log.d(TAG, "Camera launched")
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo", e)
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
        Log.d(TAG, "Gallery opened")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("CHAT_${timeStamp}_", ".jpg", storageDir)
    }

    private fun uploadImageMessage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return

        // Show progress
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        val messageId = database.reference.child("messages").child(chatRoomId).push().key ?: return
        val imageRef = storage.reference.child("chat_images/$chatRoomId/$messageId.jpg")

        Log.d(TAG, "Uploading image message...")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val message = ChatMessage(
                        id = messageId,
                        chatRoomId = chatRoomId,
                        senderId = currentUser.uid,
                        senderName = currentUserName,
                        message = "", // Empty for image messages
                        messageType = "image",
                        imageUrl = downloadUri.toString(),
                        timestamp = System.currentTimeMillis()
                    )

                    database.reference.child("messages").child(chatRoomId).child(messageId).setValue(message)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSend.isEnabled = true
                            updateLastMessage("📷 Photo")
                            Log.d(TAG, "Image message sent successfully")
                        }
                        .addOnFailureListener { e ->
                            handleUploadError("Failed to send image message", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                handleUploadError("Failed to upload image", e)
            }
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                Log.d(TAG, "Image upload progress: $progress%")
            }
    }

    private fun recordVoiceNote() {
        // TODO: Implement voice recording functionality
        Toast.makeText(this, "Voice notes - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun playVoiceMessage(message: ChatMessage) {
        // TODO: Implement voice message playback
        Toast.makeText(this, "Playing voice message - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun openImageViewer(imageUrl: String) {
        val intent = Intent(this, PhotoViewerActivity::class.java)
        intent.putExtra("imageUrl", imageUrl)
        intent.putExtra("photoId", "")
        startActivity(intent)
    }

    private fun markMessageAsRead(message: ChatMessage) {
        val currentUser = auth.currentUser ?: return

        val updatedReadBy = message.readBy.toMutableMap()
        updatedReadBy[currentUser.uid] = System.currentTimeMillis()

        database.reference.child("messages").child(chatRoomId).child(message.id)
            .child("readBy").setValue(updatedReadBy)
    }

    private fun updateLastMessage(lastMessage: String) {
        val updates = hashMapOf<String, Any>(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            "lastMessageSender" to currentUserName
        )

        database.reference.child("chatRooms").child(chatRoomId).updateChildren(updates)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating last message", e)
            }
    }

    private fun handleUploadError(message: String, exception: Exception) {
        binding.progressBar.visibility = View.GONE
        binding.btnSend.isEnabled = true
        Log.e(TAG, message, exception)
        Toast.makeText(this, "$message: ${exception.message}", Toast.LENGTH_SHORT).show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Storage permission required to access gallery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatActivity destroyed")
    }
}
