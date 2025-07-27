package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gaurav.shaadisaathi.databinding.ItemChatMessageReceivedBinding
import com.gaurav.shaadisaathi.databinding.ItemChatMessageSentBinding
import com.gaurav.shaadisaathi.databinding.ItemVoiceMessageBinding
import com.gaurav.shaadisaathi.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String,
    private val onImageClick: (String) -> Unit = {},
    private val onVoicePlay: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT_TEXT = 1
        private const val VIEW_TYPE_RECEIVED_TEXT = 2
        private const val VIEW_TYPE_SENT_IMAGE = 3
        private const val VIEW_TYPE_RECEIVED_IMAGE = 4
        private const val VIEW_TYPE_SENT_VOICE = 5
        private const val VIEW_TYPE_RECEIVED_VOICE = 6
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val isSent = message.senderId == currentUserId

        return when (message.messageType) {
            "text" -> if (isSent) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
            "image" -> if (isSent) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
            "voice" -> if (isSent) VIEW_TYPE_SENT_VOICE else VIEW_TYPE_RECEIVED_VOICE
            else -> if (isSent) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT, VIEW_TYPE_SENT_IMAGE -> {
                val binding = ItemChatMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED_TEXT, VIEW_TYPE_RECEIVED_IMAGE -> {
                val binding = ItemChatMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_SENT_VOICE -> {
                val binding = ItemVoiceMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentVoiceMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED_VOICE -> {
                val binding = ItemVoiceMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedVoiceMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SentVoiceMessageViewHolder -> holder.bind(message)
            is ReceivedVoiceMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    // Sent Text/Image Message ViewHolder
    inner class SentMessageViewHolder(private val binding: ItemChatMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvTime.text = time

            when (message.messageType) {
                "text" -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.ivImage.visibility = View.GONE
                    binding.tvMessage.text = message.message
                }
                "image" -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.ivImage.visibility = View.VISIBLE

                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(binding.ivImage)

                    binding.ivImage.setOnClickListener {
                        onImageClick(message.imageUrl)
                    }

                    // Show caption if available
                    if (message.message.isNotEmpty()) {
                        binding.tvMessage.visibility = View.VISIBLE
                        binding.tvMessage.text = message.message
                    }
                }
            }

            // Message status indicator for sent messages
            binding.ivMessageStatus.visibility = View.VISIBLE
            if (message.isRead) {
                binding.ivMessageStatus.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                binding.ivMessageStatus.setImageResource(android.R.drawable.ic_menu_send)
            }
        }
    }

    // Received Text/Image Message ViewHolder
    inner class ReceivedMessageViewHolder(private val binding: ItemChatMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvSenderName.text = message.senderName
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvTime.text = time

            when (message.messageType) {
                "text" -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.ivImage.visibility = View.GONE
                    binding.tvMessage.text = message.message
                }
                "image" -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.ivImage.visibility = View.VISIBLE

                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(binding.ivImage)

                    binding.ivImage.setOnClickListener {
                        onImageClick(message.imageUrl)
                    }

                    // Show caption if available
                    if (message.message.isNotEmpty()) {
                        binding.tvMessage.visibility = View.VISIBLE
                        binding.tvMessage.text = message.message
                    }
                }
            }
        }
    }

    // Sent Voice Message ViewHolder
    inner class SentVoiceMessageViewHolder(private val binding: ItemVoiceMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // Position container on the right for sent messages
            val layoutParams = binding.voiceMessageContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            binding.voiceMessageContainer.layoutParams = layoutParams

            // Hide sender name for sent messages
            binding.tvSenderName.visibility = View.GONE

            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvTime.text = time

            // Set voice duration (you might want to store this in the message)
            binding.tvVoiceDuration.text = "0:15" // Replace with actual duration

            // Handle play/pause button
            binding.btnPlayPause.setOnClickListener {
                onVoicePlay(message)
                togglePlayPauseButton(message)
            }

            // Show message status
            binding.ivMessageStatus.visibility = View.VISIBLE
            if (message.isRead) {
                binding.ivMessageStatus.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                binding.ivMessageStatus.setImageResource(android.R.drawable.ic_menu_send)
            }
        }
    }

    // Received Voice Message ViewHolder
    inner class ReceivedVoiceMessageViewHolder(private val binding: ItemVoiceMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // Position container on the left for received messages
            val layoutParams = binding.voiceMessageContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            binding.voiceMessageContainer.layoutParams = layoutParams

            // Show sender name for received messages
            binding.tvSenderName.visibility = View.VISIBLE
            binding.tvSenderName.text = message.senderName

            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvTime.text = time

            // Set voice duration
            binding.tvVoiceDuration.text = "0:15" // Replace with actual duration

            // Handle play/pause button
            binding.btnPlayPause.setOnClickListener {
                onVoicePlay(message)
                togglePlayPauseButton(message)
            }

            // Hide message status for received messages
            binding.ivMessageStatus.visibility = View.GONE
        }
    }

    private fun togglePlayPauseButton(message: ChatMessage) {
        // This would be used to toggle between play and pause icons
        // You'd need to implement actual media player logic here
        // For now, just a placeholder
    }

    // Helper function to format voice message duration
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}
