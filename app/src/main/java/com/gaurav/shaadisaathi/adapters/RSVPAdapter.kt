package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemRsvpBinding
import com.gaurav.shaadisaathi.models.RSVP
import java.text.SimpleDateFormat
import java.util.*

class RSVPAdapter(
    private val rsvps: List<RSVP>
) : RecyclerView.Adapter<RSVPAdapter.RSVPViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RSVPViewHolder {
        val binding = ItemRsvpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RSVPViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RSVPViewHolder, position: Int) {
        holder.bind(rsvps[position])
    }

    override fun getItemCount(): Int = rsvps.size

    inner class RSVPViewHolder(private val binding: ItemRsvpBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rsvp: RSVP) {
            binding.tvGuestName.text = rsvp.guestName
            binding.tvRSVPStatus.text = rsvp.status.capitalize()
            binding.tvEventName.text = "Event: ${rsvp.eventId}" // You can load event name from Firestore

            if (rsvp.respondedAt > 0) {
                val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(rsvp.respondedAt))
                binding.tvRSVPDate.text = "Responded on: $date"
            } else {
                binding.tvRSVPDate.text = "No response yet"
            }
        }
    }
}
