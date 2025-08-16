package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.GuestSelectionAdapter // FIX: Proper import
import com.gaurav.shaadisaathi.databinding.ActivityCreateChatRoomBinding
import com.gaurav.shaadisaathi.models.ChatRoom
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.ChatRepository
import com.gaurav.shaadisaathi.repository.GuestRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CreateChatRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateChatRoomBinding
    private lateinit var guestSelectionAdapter: GuestSelectionAdapter // FIX: Proper type
    private val guestList = mutableListOf<Guest>()
    private val selectedGuests = mutableListOf<Guest>()
    private val chatRepository = ChatRepository()
    private val guestRepository = GuestRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadGuests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar) // FIX: Now references correct toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Create Chat Room"
        }
    }

    private fun setupRecyclerView() {
        guestSelectionAdapter = GuestSelectionAdapter(
            guests = guestList,
            onGuestSelected = { guest: Guest, isSelected: Boolean -> // FIX: Explicit types
                if (isSelected) {
                    selectedGuests.add(guest)
                } else {
                    selectedGuests.remove(guest)
                }
                updateSelectedCount()
            }
        )

        binding.recyclerViewGuests.apply { // FIX: Now references correct RecyclerView
            layoutManager = LinearLayoutManager(this@CreateChatRoomActivity)
            adapter = guestSelectionAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateChatRoom.setOnClickListener { // FIX: Now references correct button
            createChatRoom()
        }

        binding.btnSelectAll.setOnClickListener { // FIX: Now references correct button
            selectAllGuests()
        }

        binding.btnDeselectAll.setOnClickListener { // FIX: Now references correct button
            deselectAllGuests()
        }
    }

    private fun loadGuests() {
        lifecycleScope.launch {
            try {
                val result = guestRepository.getAllGuests()
                if (result.isSuccess) {
                    val guests = result.getOrNull() ?: emptyList()
                    guestList.clear()
                    guestList.addAll(guests.sortedBy { it.name })
                    guestSelectionAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@CreateChatRoomActivity, "Error loading guests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateChatRoomActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedCount() {
        binding.tvSelectedCount.text = "Selected: ${selectedGuests.size}" // FIX: Now references correct TextView
        binding.btnCreateChatRoom.isEnabled = selectedGuests.isNotEmpty() // FIX: Now references correct button
    }

    private fun selectAllGuests() {
        selectedGuests.clear()
        selectedGuests.addAll(guestList)
        guestSelectionAdapter.selectAll()
        updateSelectedCount()
    }

    private fun deselectAllGuests() {
        selectedGuests.clear()
        guestSelectionAdapter.deselectAll()
        updateSelectedCount()
    }

    private fun createChatRoom() {
        val chatRoomName = binding.etChatRoomName.text.toString().trim() // FIX: Now references correct EditText
        val chatRoomDescription = binding.etChatRoomDescription.text.toString().trim() // FIX: Now references correct EditText

        if (chatRoomName.isEmpty()) {
            binding.etChatRoomName.error = "Chat room name is required" // FIX: Now references correct EditText
            return
        }

        if (selectedGuests.isEmpty()) {
            Toast.makeText(this, "Please select at least one guest", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val memberIds = selectedGuests.map { it.id } + currentUser.uid

                val chatRoom = ChatRoom(
                    id = System.currentTimeMillis().toString(),
                    name = chatRoomName,
                    description = chatRoomDescription,
                    type = "group",
                    eventId = "",
                    hostId = currentUser.uid,
                    members = memberIds,
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis(),
                    lastMessageSender = "",
                    createdAt = System.currentTimeMillis(),
                    isActive = true
                )

                val result = chatRepository.createChatRoom(chatRoom)
                if (result.isSuccess) {
                    Toast.makeText(this@CreateChatRoomActivity, "Chat room created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateChatRoomActivity, "Error creating chat room", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateChatRoomActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
