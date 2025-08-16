package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemChatListBinding
import com.gaurav.shaadisaathi.models.ChatItem // FIX: Add this import
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val chats: MutableList<ChatItem>,
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<ChatItem>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(private val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatItem) {
            binding.apply {
                tvGuestName.text = chat.guestName
                tvLastMessage.text = if (chat.lastMessage.isNotEmpty()) {
                    chat.lastMessage
                } else {
                    "No messages yet"
                }

                // Set guest initial
                val initial = chat.guestName.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvGuestInitial.text = initial

                // Format time
                if (chat.lastMessageTime > 0) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    tvLastMessageTime.text = timeFormat.format(Date(chat.lastMessageTime))
                } else {
                    tvLastMessageTime.text = ""
                }

                // Unread count
                if (chat.unreadCount > 0) {
                    chipUnreadCount.visibility = View.VISIBLE
                    chipUnreadCount.text = chat.unreadCount.toString()
                } else {
                    chipUnreadCount.visibility = View.GONE
                }

                // Online status
                viewOnlineStatus.visibility = if (chat.isOnline) View.VISIBLE else View.GONE

                // Click listener
                root.setOnClickListener { onChatClick(chat) }
            }
        }
    }
}
