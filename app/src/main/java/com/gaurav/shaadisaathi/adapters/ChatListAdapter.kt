package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemChatRoomBinding
import com.gaurav.shaadisaathi.models.ChatRoom
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val chatRooms: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

    override fun getItemCount(): Int = chatRooms.size

    inner class ChatRoomViewHolder(private val binding: ItemChatRoomBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoom) {
            binding.tvChatRoomName.text = chatRoom.name
            binding.tvLastMessage.text = chatRoom.lastMessage.ifEmpty { "No messages yet" }

            if (chatRoom.lastMessageTime > 0) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chatRoom.lastMessageTime))
                binding.tvTime.text = time
            }

            binding.tvChatType.text = chatRoom.type.capitalize()

            binding.root.setOnClickListener {
                onItemClick(chatRoom)
            }
        }
    }
}
