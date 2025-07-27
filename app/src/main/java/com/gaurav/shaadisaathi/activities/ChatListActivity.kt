package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.adapters.ChatListAdapter
import com.gaurav.shaadisaathi.databinding.ActivityChatListBinding
import com.gaurav.shaadisaathi.models.ChatRoom

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var chatListAdapter: ChatListAdapter
    private val chatRoomList = mutableListOf<ChatRoom>()
    private val TAG = "ChatListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadChatRooms()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.fabCreateChat.setOnClickListener {
            startActivity(Intent(this, CreateChatRoomActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter(chatRoomList) { chatRoom ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chatRoomId", chatRoom.id)
            intent.putExtra("chatRoomName", chatRoom.name)
            startActivity(intent)
        }

        binding.recyclerViewChatList.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = chatListAdapter
        }
    }

    private fun loadChatRooms() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        // First get user role to determine which chat rooms to show
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                val userRole = userDoc.getString("role")

                database.reference.child("chatRooms")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            binding.progressBar.visibility = View.GONE
                            chatRoomList.clear()

                            for (chatSnapshot in snapshot.children) {
                                val chatRoom = chatSnapshot.getValue(ChatRoom::class.java)
                                chatRoom?.let {
                                    // Show chat rooms where user is a member or is host
                                    if (it.members.contains(currentUser.uid) ||
                                        (userRole == "host" && it.hostId == currentUser.uid)) {
                                        chatRoomList.add(it)
                                    }
                                }
                            }

                            // Sort by last message time
                            chatRoomList.sortByDescending { it.lastMessageTime }
                            chatListAdapter.notifyDataSetChanged()

                            updateEmptyState()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error loading chat rooms", error.toException())
                            binding.progressBar.visibility = View.GONE
                            updateEmptyState()
                        }
                    })
            }
    }

    private fun updateEmptyState() {
        if (chatRoomList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewChatList.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewChatList.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadChatRooms()
    }
}
