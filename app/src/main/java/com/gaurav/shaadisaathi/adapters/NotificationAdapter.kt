package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemNotificationBinding
import com.gaurav.shaadisaathi.models.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: MutableList<NotificationItem>, // FIX: Use NotificationItem
    private val onNotificationClick: (NotificationItem) -> Unit // FIX: Add missing parameter
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            notifyItemChanged(index)
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message

                // Format timestamp
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                tvNotificationTime.text = dateFormat.format(Date(notification.timestamp))

                // Set notification icon based on type
                ivNotificationIcon.setImageResource(
                    when (notification.type) {
                        "invitation" -> R.drawable.ic_invitation
                        "rsvp_update" -> R.drawable.ic_placeholder
                        "reminder" -> R.drawable.ic_placeholder
                        "chat" -> R.drawable.ic_chat
                        else -> R.drawable.ic_notification
                    }
                )

                // Show unread indicator
                viewUnreadIndicator.visibility = if (!notification.isRead) View.VISIBLE else View.GONE

                // Set background based on read status
                root.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        if (notification.isRead) R.color.colorAccent else R.color.colorPrimaryLight
                    )
                )

                // Priority indicator
                when (notification.priority) {
                    "high" -> {
                        root.strokeColor = ContextCompat.getColor(itemView.context, R.color.status_declined)
                        root.strokeWidth = 4
                    }
                    "normal" -> {
                        root.strokeColor = ContextCompat.getColor(itemView.context, R.color.colorSecondary)
                        root.strokeWidth = 2
                    }
                    else -> {
                        root.strokeWidth = 0
                    }
                }

                // Click listener
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }
    }
}
