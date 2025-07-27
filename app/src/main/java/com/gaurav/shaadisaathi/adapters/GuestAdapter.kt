package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemGuestBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestAdapter(
    private val guests: List<Guest>,
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

    inner class GuestViewHolder(private val binding: ItemGuestBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.tvGuestName.text = guest.name
            binding.tvGuestCategory.text = guest.category
            binding.tvGuestContact.text = if (guest.email.isNotEmpty()) guest.email else guest.phone

            binding.tvPlusOne.text = if (guest.plusOne) "+1" else ""

            binding.root.setOnClickListener {
                onItemClick(guest)
            }
        }
    }
}
