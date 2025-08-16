package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemRsvpGuestBinding
import com.gaurav.shaadisaathi.models.Guest

class RSVPAdapter(
    private val guests: MutableList<Guest>,
    private val onStatusUpdate: (Guest, String) -> Unit,
    private val onSendReminder: (Guest) -> Unit
) : RecyclerView.Adapter<RSVPAdapter.RSVPViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RSVPViewHolder {
        val binding = ItemRsvpGuestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RSVPViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RSVPViewHolder, position: Int) {
        holder.bind(guests[position])
    }

    override fun getItemCount(): Int = guests.size

    fun updateGuests(newGuests: List<Guest>) {
        guests.clear()
        guests.addAll(newGuests)
        notifyDataSetChanged()
    }

    inner class RSVPViewHolder(private val binding: ItemRsvpGuestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.apply {
                tvGuestName.text = guest.name
                tvGuestCategory.text = guest.category.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }

                // Contact info
                tvGuestContact.text = when {
                    guest.phone.isNotEmpty() -> guest.phone
                    guest.email.isNotEmpty() -> guest.email
                    else -> "No contact info"
                }

                // Set guest initial
                val initial = guest.name.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvGuestInitial.text = initial

                // Setup spinner
                val statusOptions = arrayOf("Pending", "Confirmed", "Declined")
                val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, statusOptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRsvpStatus.adapter = adapter

                // Set current status
                val currentStatusIndex = when (guest.rsvpStatus) {
                    "confirmed" -> 1
                    "declined" -> 2
                    else -> 0
                }
                spinnerRsvpStatus.setSelection(currentStatusIndex)

                // Status change listener
                spinnerRsvpStatus.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val newStatus = when (position) {
                            1 -> "confirmed"
                            2 -> "declined"
                            else -> "pending"
                        }
                        if (newStatus != guest.rsvpStatus) {
                            onStatusUpdate(guest, newStatus)
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                })

                // Plus one info
                if (guest.hasPlusOne) {
                    layoutPlusOne.visibility = View.VISIBLE
                    tvPlusOneName.text = if (guest.plusOneName.isNotEmpty())
                        guest.plusOneName else "Plus One Guest"

                    chipPlusOneStatus.apply {
                        if (guest.plusOneConfirmed) {
                            text = "Confirmed"
                            setChipBackgroundColorResource(R.color.status_confirmed)
                        } else {
                            text = "Pending"
                            setChipBackgroundColorResource(R.color.status_pending)
                        }
                    }
                } else {
                    layoutPlusOne.visibility = View.GONE
                }

                // RSVP response date
                if (guest.rsvpResponseAt > 0) {
                    tvResponseDate.visibility = View.VISIBLE
                    val responseDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(guest.rsvpResponseAt))
                    tvResponseDate.text = "Responded: $responseDate"
                } else {
                    tvResponseDate.visibility = View.GONE
                }

                // Send reminder button
                btnSendReminder.setOnClickListener { onSendReminder(guest) }

                // Show reminder button only for pending guests
                btnSendReminder.visibility = if (guest.rsvpStatus == "pending") View.VISIBLE else View.GONE

                // Set card background based on status
                val cardColor = when (guest.rsvpStatus) {
                    "confirmed" -> ContextCompat.getColor(itemView.context, R.color.colorBackground)
                    "declined" -> ContextCompat.getColor(itemView.context, R.color.colorBackground)
                    else -> ContextCompat.getColor(itemView.context, R.color.colorPrimaryLight)
                }
                root.setCardBackgroundColor(cardColor)
            }
        }
    }
}
