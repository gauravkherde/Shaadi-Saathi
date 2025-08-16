package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemGuestSelectionBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestSelectionAdapter(
    private val guests: MutableList<Guest>,
    private val onGuestSelected: (Guest, Boolean) -> Unit
) : RecyclerView.Adapter<GuestSelectionAdapter.GuestViewHolder>() {

    private val selectedGuests = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val binding = ItemGuestSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        holder.bind(guests[position])
    }

    override fun getItemCount(): Int = guests.size

    fun selectAll() {
        selectedGuests.clear()
        selectedGuests.addAll(guests.map { it.id })
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedGuests.clear()
        notifyDataSetChanged()
    }

    inner class GuestViewHolder(private val binding: ItemGuestSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.apply {
                tvGuestName.text = guest.name
                tvGuestCategory.text = guest.getCategoryDisplayName()
                tvGuestContact.text = when {
                    guest.phone.isNotEmpty() -> guest.phone
                    guest.email.isNotEmpty() -> guest.email
                    else -> "No contact"
                }

                // Set guest initial
                val initial = guest.name.firstOrNull()?.toString()?.uppercase() ?: "G"
                tvGuestInitial.text = initial

                // Checkbox state
                val isSelected = selectedGuests.contains(guest.id)
                checkboxSelect.isChecked = isSelected

                // Set background color based on selection
                root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        if (isSelected) R.color.colorPrimaryLight else R.color.colorAccent
                    )
                )

                // Click listeners
                checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGuests.add(guest.id)
                    } else {
                        selectedGuests.remove(guest.id)
                    }
                    onGuestSelected(guest, isChecked)

                    // Update background color
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(
                            itemView.context,
                            if (isChecked) R.color.colorPrimaryLight else R.color.colorAccent
                        )
                    )
                }

                root.setOnClickListener {
                    checkboxSelect.isChecked = !checkboxSelect.isChecked
                }
            }
        }
    }
}
