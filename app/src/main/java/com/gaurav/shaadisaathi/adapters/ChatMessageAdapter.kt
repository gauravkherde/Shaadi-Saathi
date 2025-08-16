package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemChatMessageIncomingBinding
import com.gaurav.shaadisaathi.databinding.ItemChatMessageOutgoingBinding
import com.gaurav.shaadisaathi.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val currentUserId: String,
    private val onImageClick: ((ChatMessage) -> Unit)? = null,
    private val onVoicePlay: ((ChatMessage) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> {
                val binding = ItemChatMessageOutgoingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                OutgoingMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageIncomingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                IncomingMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OutgoingMessageViewHolder -> holder.bind(messages[position])
            is IncomingMessageViewHolder -> holder.bind(messages[position])
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    inner class OutgoingMessageViewHolder(
        private val binding: ItemChatMessageOutgoingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                tvMessageTime.text = timeFormat.format(Date(message.timestamp))

                // Message status icon
                ivMessageStatus.setImageResource(
                    when {
                        message.isRead -> com.gaurav.shaadisaathi.R.drawable.ic_message_read
                        message.isDelivered -> com.gaurav.shaadisaathi.R.drawable.ic_message_delivered
                        else -> com.gaurav.shaadisaathi.R.drawable.ic_message_sent
                    }
                )

                // Handle image messages
                if (message.messageType == "image" && onImageClick != null) {
                    root.setOnClickListener { onImageClick.invoke(message) }
                }

                // Handle voice messages
                if (message.messageType == "voice" && onVoicePlay != null) {
                    root.setOnClickListener { onVoicePlay.invoke(message) }
                }
            }
        }
    }

    inner class IncomingMessageViewHolder(
        private val binding: ItemChatMessageIncomingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvSenderName.text = message.senderName

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                tvMessageTime.text = timeFormat.format(Date(message.timestamp))

                // Set sender initial
                val initial = message.senderName.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvSenderInitial.text = initial

                // Handle image messages
                if (message.messageType == "image" && onImageClick != null) {
                    root.setOnClickListener { onImageClick.invoke(message) }
                }

                // Handle voice messages
                if (message.messageType == "voice" && onVoicePlay != null) {
                    root.setOnClickListener { onVoicePlay.invoke(message) }
                }
            }
        }
    }
}
