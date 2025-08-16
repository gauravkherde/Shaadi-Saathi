package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemGuestListBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestListAdapter(
    private val guests: MutableList<Guest>,
    private val onGuestClick: (Guest) -> Unit,
    private val onCallClick: (Guest) -> Unit,
    private val onEmailClick: (Guest) -> Unit
) : RecyclerView.Adapter<GuestListAdapter.GuestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val binding = ItemGuestListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
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

    inner class GuestViewHolder(private val binding: ItemGuestListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.apply {
                tvGuestName.text = guest.name
                tvGuestCategory.text = guest.getCategoryDisplayName()

                // Contact info
                tvGuestPhone.text = if (guest.phone.isNotEmpty()) guest.phone else "No phone"
                tvGuestEmail.text = if (guest.email.isNotEmpty()) guest.email else "No email"

                // Set guest initial
                val initial = guest.name.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvGuestInitial.text = initial

                // RSVP Status chip
                chipRsvpStatus.apply {
                    text = guest.rsvpStatus.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                    setChipBackgroundColorResource(
                        when (guest.rsvpStatus) {
                            "confirmed" -> R.color.status_confirmed
                            "declined" -> R.color.status_declined
                            else -> R.color.status_pending
                        }
                    )
                }

                // Click listeners
                root.setOnClickListener { onGuestClick(guest) }

                // Enable/disable action buttons based on available contact info
                val hasPhone = guest.phone.isNotEmpty()
                val hasEmail = guest.email.isNotEmpty()

                if (hasPhone) {
                    root.setOnLongClickListener {
                        onCallClick(guest)
                        true
                    }
                }

                if (hasEmail) {
                    root.setOnClickListener {
                        if (it.context.packageManager.hasSystemFeature("android.hardware.touchscreen")) {
                            onEmailClick(guest)
                        } else {
                            onGuestClick(guest)
                        }
                    }
                }
            }
        }
    }
}
