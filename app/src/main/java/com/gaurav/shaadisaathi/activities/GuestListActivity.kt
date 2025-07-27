package com.gaurav.shaadisaathi.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.adapters.GuestAdapter
import com.gaurav.shaadisaathi.databinding.ActivityGuestListBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestListBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var guestAdapter: GuestAdapter
    private val guestList = mutableListOf<Guest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadGuests()

        binding.fabAddGuest.setOnClickListener {
            startActivity(Intent(this, AddGuestActivity::class.java))
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        guestAdapter = GuestAdapter(guestList) { guest ->
            // Handle guest item click - edit or view details
            val intent = Intent(this, AddGuestActivity::class.java)
            intent.putExtra("guestId", guest.id)
            startActivity(intent)
        }

        binding.recyclerViewGuests.apply {
            layoutManager = LinearLayoutManager(this@GuestListActivity)
            adapter = guestAdapter
        }
    }

    private fun loadGuests() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("guests")
            .whereEqualTo("hostId", currentUser.uid)
            .orderBy("name")
            .addSnapshotListener { snapshots, e ->
                binding.progressBar.visibility = View.GONE

                if (e != null) {
                    return@addSnapshotListener
                }

                guestList.clear()
                snapshots?.documents?.forEach { doc ->
                    val guest = doc.toObject(Guest::class.java)
                    guest?.let { guestList.add(it) }
                }

                guestAdapter.notifyDataSetChanged()

                // Show/hide empty state
                if (guestList.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewGuests.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewGuests.visibility = View.VISIBLE
                }
            }
    }
}
