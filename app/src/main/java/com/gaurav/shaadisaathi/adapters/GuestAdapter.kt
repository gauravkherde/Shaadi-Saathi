package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemGuestBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestAdapter(
    private val guests: MutableList<Guest>, // FIX: Use MutableList for updates
    private val onItemClick: (Guest) -> Unit
) : RecyclerView.Adapter<GuestAdapter.GuestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val binding = ItemGuestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        holder.bind(guests[position])
    }

    override fun getItemCount(): Int = guests.size

    fun updateGuests(newGuests: List<Guest>) {
        guests.clear()
        guests.addAll(newGuests)
        notifyDataSetChanged()
    }

    inner class GuestViewHolder(private val binding: ItemGuestBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.apply {
                tvGuestName.text = guest.name
                tvGuestCategory.text = guest.getCategoryDisplayName() // Use helper method

                // Display contact information
                tvGuestContact.text = when {
                    guest.email.isNotEmpty() -> guest.email
                    guest.phone.isNotEmpty() -> guest.phone
                    else -> "No contact info"
                }

                // FIX: Use hasPlusOne instead of plusOne
                tvPlusOne.text = if (guest.hasPlusOne) "+1" else ""

                // Set guest initial
                val initial = guest.name.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvGuestInitial.text = initial

                // RSVP Status chip
                chipRsvpStatus.text = guest.rsvpStatus.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }

                // Set chip background color based on RSVP status
                val chipBackgroundColor = when (guest.rsvpStatus.lowercase()) {
                    "confirmed" -> com.gaurav.shaadisaathi.R.color.status_confirmed
                    "declined" -> com.gaurav.shaadisaathi.R.color.status_declined
                    else -> com.gaurav.shaadisaathi.R.color.status_pending
                }
                chipRsvpStatus.setChipBackgroundColorResource(chipBackgroundColor)

                root.setOnClickListener {
                    onItemClick(guest)
                }
            }
        }
    }
}
