package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemNotificationBinding
import com.gaurav.shaadisaathi.models.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message

            // Format timestamp
            val timeAgo = getTimeAgo(notification.timestamp)
            binding.tvNotificationTime.text = timeAgo

            // Show unread indicator
            if (!notification.isRead) {
                binding.unreadIndicator.visibility = View.VISIBLE
                binding.root.alpha = 1.0f
            } else {
                binding.unreadIndicator.visibility = View.GONE
                binding.root.alpha = 0.7f
            }

            // Set notification icon based on type
            when (notification.type) {
                "chat" -> {
                    binding.ivNotificationIcon.setImageResource(android.R.drawable.ic_menu_share)
                }
                "photo" -> {
                    binding.ivNotificationIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                "event" -> {
                    binding.ivNotificationIcon.setImageResource(android.R.drawable.ic_menu_today)
                }
                "rsvp" -> {
                    binding.ivNotificationIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                }
                else -> {
                    binding.ivNotificationIcon.setImageResource(android.R.drawable.ic_dialog_info)
                }
            }

            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
