package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.ChatMessageAdapter
import com.gaurav.shaadisaathi.databinding.ActivityChatBinding
import com.gaurav.shaadisaathi.models.ChatMessage
import com.gaurav.shaadisaathi.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private val chatRepository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    private var chatRoomId: String = ""
    private var guestName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        chatRoomId = intent.getStringExtra("chatRoomId") ?: ""
        guestName = intent.getStringExtra("guestName") ?: "Guest"

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = guestName
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""

        chatAdapter = ChatMessageAdapter(
            messages = messageList,
            currentUserId = currentUserId,
            onImageClick = { message ->
                // Handle image click
                handleImageClick(message)
            },
            onVoicePlay = { message ->
                // Handle voice message play
                handleVoicePlay(message)
            }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        binding.btnAttachPhoto.setOnClickListener {
            // Handle photo attachment
            attachPhoto()
        }

        binding.btnVoiceMessage.setOnClickListener {
            // Handle voice message
            recordVoiceMessage()
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val result = chatRepository.getMessages(chatRoomId)
                if (result.isSuccess) {
                    val messages = result.getOrNull() ?: emptyList()
                    messageList.clear()
                    messageList.addAll(messages.sortedBy { it.timestamp })
                    chatAdapter.notifyDataSetChanged()

                    // Scroll to bottom
                    if (messages.isNotEmpty()) {
                        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Error loading messages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val message = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    message = messageText,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    senderName = currentUser.displayName ?: "You",
                    senderId = currentUser.uid,
                    chatRoomId = chatRoomId, // FIX: Use chatRoomId parameter
                    isDelivered = false,
                    isRead = false,
                    readBy = emptyList(),
                    messageType = "text"
                )

                val result = chatRepository.sendMessage(message)
                if (result.isSuccess) {
                    binding.etMessage.text?.clear()
                    // FIX: Add message to list properly
                    messageList.add(message)
                    chatAdapter.notifyItemInserted(messageList.size - 1)
                    binding.recyclerViewMessages.scrollToPosition(messageList.size - 1)
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImageClick(message: ChatMessage) {
        // Handle image message click
        if (message.imageUrl.isNotEmpty()) {
            // Open image viewer
            // TODO: Implement image viewer
        }
    }

    private fun handleVoicePlay(message: ChatMessage) {
        // Handle voice message play
        if (message.voiceUrl.isNotEmpty()) {
            // Play voice message
            // TODO: Implement voice player
        }
    }

    private fun attachPhoto() {
        // TODO: Implement photo attachment
        Toast.makeText(this, "Photo attachment coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun recordVoiceMessage() {
        // TODO: Implement voice recording
        Toast.makeText(this, "Voice messages coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun markMessagesAsRead() {
        lifecycleScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch

                val unreadMessages = messageList.filter {
                    !it.readBy.contains(currentUserId) && it.senderId != currentUserId
                }

                for (message in unreadMessages) {
                    val updatedMessage = message.copy(
                        readBy = message.readBy + currentUserId,
                        isRead = true
                    )
                    chatRepository.updateMessage(updatedMessage)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    override fun onResume() {
        super.onResume()
        markMessagesAsRead()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
