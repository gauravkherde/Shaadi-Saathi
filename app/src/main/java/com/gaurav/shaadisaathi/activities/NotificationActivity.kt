package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.NotificationAdapter
import com.gaurav.shaadisaathi.databinding.ActivityNotificationBinding
import com.gaurav.shaadisaathi.models.NotificationItem
import com.gaurav.shaadisaathi.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>() // FIX: Use NotificationItem
    private val notificationRepository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Notifications"
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications = notificationList,
            onNotificationClick = { notification: NotificationItem ->
                // Handle notification click
                markAsRead(notification)
                handleNotificationClick(notification)
            }
        )

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }
    }

    private fun loadNotifications() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@NotificationActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val result = notificationRepository.getNotifications(currentUser.uid)
                if (result.isSuccess) {
                    val notifications = result.getOrNull() ?: emptyList()
                    notificationList.clear()
                    notificationList.addAll(notifications.sortedByDescending { it.timestamp })
                    notificationAdapter.notifyDataSetChanged()

                    updateEmptyState(notifications.isEmpty())
                } else {
                    Toast.makeText(this@NotificationActivity, "Error loading notifications", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun markAsRead(notification: NotificationItem) {
        if (!notification.isRead) {
            lifecycleScope.launch {
                try {
                    val updatedNotification = notification.copy(isRead = true)
                    val result = notificationRepository.updateNotification(updatedNotification)
                    if (result.isSuccess) {
                        notificationAdapter.markAsRead(notification.id)
                    }
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }

    private fun handleNotificationClick(notification: NotificationItem) {
        when (notification.type) {
            "invitation" -> {
                // Navigate to guest detail or invitation screen
                val intent = Intent(this, GuestDetailActivity::class.java)
                intent.putExtra("guestId", notification.actionData)
                startActivity(intent)
            }
            "rsvp_update" -> {
                // Navigate to RSVP management screen
                val intent = Intent(this, RSVPActivity::class.java)
                startActivity(intent)
            }
            "reminder" -> {
                // Navigate to relevant screen based on action data
                handleReminderClick(notification.actionData)
            }
            "chat" -> {
                // Navigate to chat screen
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("chatRoomId", notification.actionData)
                startActivity(intent)
            }
            else -> {
                // Default action - just mark as read
                Toast.makeText(this, "Notification opened", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleReminderClick(actionData: String) {
        // Parse action data and navigate accordingly
        when {
            actionData.startsWith("event_") -> {
                val eventId = actionData.substring(6)
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("eventId", eventId)
                startActivity(intent)
            }
            actionData.startsWith("vendor_") -> {
                val vendorId = actionData.substring(7)
                val intent = Intent(this, VendorDetailActivity::class.java)
                intent.putExtra("vendorId", vendorId)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Reminder notification", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewNotifications.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewNotifications.visibility = android.view.View.VISIBLE
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val result = notificationRepository.markAllAsRead(currentUser.uid)
                if (result.isSuccess) {
                    loadNotifications() // Refresh the list
                    Toast.makeText(this@NotificationActivity, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NotificationActivity, "Error marking notifications as read", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllNotifications() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all notifications? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                performClearAll()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performClearAll() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val result = notificationRepository.clearAllNotifications(currentUser.uid)
                if (result.isSuccess) {
                    notificationList.clear()
                    notificationAdapter.notifyDataSetChanged()
                    updateEmptyState(true)
                    Toast.makeText(this@NotificationActivity, "All notifications cleared", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NotificationActivity, "Error clearing notifications", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mark_all_read -> {
                markAllAsRead()
                true
            }
            R.id.action_clear_all -> {
                clearAllNotifications()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications() // Refresh notifications when returning to screen
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
