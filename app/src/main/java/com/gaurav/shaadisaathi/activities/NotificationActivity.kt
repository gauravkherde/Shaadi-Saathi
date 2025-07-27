package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.adapters.NotificationAdapter
import com.gaurav.shaadisaathi.databinding.ActivityNotificationBinding
import com.gaurav.shaadisaathi.models.Notification

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadNotifications()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(notificationList) { notification ->
            // Handle notification click
            markAsRead(notification)
        }

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
        }
    }

    private fun loadNotifications() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("notifications")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                binding.progressBar.visibility = View.GONE

                if (e != null) {
                    return@addSnapshotListener
                }

                notificationList.clear()
                snapshots?.documents?.forEach { doc ->
                    val notification = doc.toObject(Notification::class.java)
                    notification?.let { notificationList.add(it) }
                }

                notificationAdapter.notifyDataSetChanged()

                if (notificationList.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewNotifications.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewNotifications.visibility = View.VISIBLE
                }
            }
    }

    private fun markAsRead(notification: Notification) {
        if (!notification.isRead) {
            firestore.collection("notifications").document(notification.id)
                .update("isRead", true)
        }
    }
}
