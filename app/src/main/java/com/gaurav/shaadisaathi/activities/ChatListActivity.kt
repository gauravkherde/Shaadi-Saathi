package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.ChatListAdapter
import com.gaurav.shaadisaathi.databinding.ActivityChatListBinding
import com.gaurav.shaadisaathi.models.ChatItem
import com.gaurav.shaadisaathi.models.ChatRoom
import com.gaurav.shaadisaathi.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var chatListAdapter: ChatListAdapter
    private val chatList = mutableListOf<ChatItem>() // FIX: Use ChatItem for adapter
    private val chatRepository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadChatRooms()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Messages"
        }
    }

    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter(
            chats = chatList,
            onChatClick = { chatItem: ChatItem -> // FIX: Explicit type annotation
                openChat(chatItem)
            }
        )

        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = chatListAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabCreateChat.setOnClickListener {
            val intent = Intent(this, CreateChatRoomActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadChatRooms()
        }
    }

    private fun loadChatRooms() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@ChatListActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val result = chatRepository.getChatRooms(currentUser.uid)
                if (result.isSuccess) {
                    val chatRooms = result.getOrNull() ?: emptyList()

                    // FIX: Convert ChatRoom to ChatItem for adapter compatibility
                    val chatItems = chatRooms.filter { chatRoom ->
                        // FIX: Check if user is member of the chat room
                        chatRoom.members.contains(currentUser.uid) || chatRoom.hostId == currentUser.uid
                    }.map { chatRoom ->
                        chatRoom.toChatItem()
                    }

                    chatList.clear()
                    chatList.addAll(chatItems)
                    chatListAdapter.notifyDataSetChanged()

                    updateEmptyState(chatItems.isEmpty())
                } else {
                    Toast.makeText(this@ChatListActivity, "Error loading chats", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewChats.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewChats.visibility = android.view.View.VISIBLE
        }
    }

    private fun openChat(chatItem: ChatItem) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatRoomId", chatItem.id)
        intent.putExtra("guestName", chatItem.guestName)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadChatRooms()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
