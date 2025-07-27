package com.gaurav.shaadisaathi.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.adapters.RSVPAdapter
import com.gaurav.shaadisaathi.databinding.ActivityRsvpBinding
import com.gaurav.shaadisaathi.models.RSVP

class RSVPActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRsvpBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var rsvpAdapter: RSVPAdapter
    private val rsvpList = mutableListOf<RSVP>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsvpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadRSVPs()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rsvpAdapter = RSVPAdapter(rsvpList)

        binding.recyclerViewRSVP.apply {
            layoutManager = LinearLayoutManager(this@RSVPActivity)
            adapter = rsvpAdapter
        }
    }

    private fun loadRSVPs() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        // For hosts, show all RSVPs for their events
        // For guests, show their own RSVPs

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                val userRole = userDoc.getString("role")

                if (userRole == "host") {
                    loadHostRSVPs(currentUser.uid)
                } else {
                    loadGuestRSVPs(currentUser.uid)
                }
            }
    }

    private fun loadHostRSVPs(hostId: String) {
        firestore.collection("rsvps")
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshots, e ->
                binding.progressBar.visibility = View.GONE

                if (e != null) {
                    return@addSnapshotListener
                }

                rsvpList.clear()
                snapshots?.documents?.forEach { doc ->
                    val rsvp = doc.toObject(RSVP::class.java)
                    rsvp?.let { rsvpList.add(it) }
                }

                rsvpAdapter.notifyDataSetChanged()

                if (rsvpList.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewRSVP.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewRSVP.visibility = View.VISIBLE
                }
            }
    }

    private fun loadGuestRSVPs(guestId: String) {
        firestore.collection("rsvps")
            .whereEqualTo("guestId", guestId)
            .addSnapshotListener { snapshots, e ->
                binding.progressBar.visibility = View.GONE

                if (e != null) {
                    return@addSnapshotListener
                }

                rsvpList.clear()
                snapshots?.documents?.forEach { doc ->
                    val rsvp = doc.toObject(RSVP::class.java)
                    rsvp?.let { rsvpList.add(it) }
                }

                rsvpAdapter.notifyDataSetChanged()

                if (rsvpList.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewRSVP.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewRSVP.visibility = View.VISIBLE
                }
            }
    }
}
