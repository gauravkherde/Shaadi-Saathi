package com.gaurav.shaadisaathi.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemGuestListBinding
import com.gaurav.shaadisaathi.models.Guest

class GuestListAdapter(
    private var guests: List<Guest>,
    private val onItemClick: (Guest) -> Unit,
    private val onEditClick: (Guest) -> Unit,
    private val onDeleteClick: (Guest) -> Unit,
    private val onRSVPClick: (Guest) -> Unit
) : RecyclerView.Adapter<GuestListAdapter.GuestViewHolder>() {

    fun updateGuests(newGuests: List<Guest>) {
        guests = newGuests
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val binding = ItemGuestListBinding.inflate(
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

    inner class GuestViewHolder(private val binding: ItemGuestListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest) {
            binding.apply {
                // Basic info
                tvGuestName.text = guest.getDisplayName()
                tvGuestPhone.text = if (guest.phone.isNotEmpty()) guest.phone else "No phone"
                tvGuestEmail.text = if (guest.email.isNotEmpty()) guest.email else "No email"
                tvGuestCategory.text = guest.getCategoryDisplayName()

                // RSVP Status
                updateRSVPStatus(guest)

                // Plus one indicator
                if (guest.hasPlusOne) {
                    tvPlusOne.visibility = View.VISIBLE
                    tvPlusOne.text = if (guest.plusOneConfirmed) {
                        "+1 (${guest.plusOneName.ifEmpty { "Guest" }})"
                    } else {
                        "+1 (Pending)"
                    }
                } else {
                    tvPlusOne.visibility = View.GONE
                }

                // Meal preference
                ivMealPreference.setImageResource(
                    when (guest.mealPreference) {
                        "veg" -> R.drawable.ic_meal_veg
                        "non-veg" -> R.drawable.ic_meal_nonveg
                        "jain" -> R.drawable.ic_meal_veg
                        "vegan" -> R.drawable.ic_meal_veg
                        else -> R.drawable.ic_meal_veg
                    }
                )

                // VIP indicator
                ivVipIndicator.visibility = if (guest.isVip) View.VISIBLE else View.GONE

                // Invitation status
                updateInvitationStatus(guest)

                // Special requirements indicator
                ivSpecialRequirements.visibility =
                    if (guest.specialRequirements.isNotEmpty()) View.VISIBLE else View.GONE

                // Click listeners
                root.setOnClickListener { onItemClick(guest) }
                btnEdit.setOnClickListener { onEditClick(guest) }
                btnDelete.setOnClickListener { onDeleteClick(guest) }
                chipRsvpStatus.setOnClickListener { onRSVPClick(guest) }

                // Long click for context menu
                root.setOnLongClickListener {
                    showContextMenu(guest)
                    true
                }
            }
        }

        private fun updateRSVPStatus(guest: Guest) {
            binding.apply {
                when (guest.rsvpStatus) {
                    "confirmed" -> {
                        chipRsvpStatus.text = "Confirmed"
                        chipRsvpStatus.setChipBackgroundColorResource(R.color.status_confirmed)
                        chipRsvpStatus.setTextColor(Color.WHITE)
                        ivRsvpIcon.setImageResource(R.drawable.ic_rsvp_confirmed)
                        ivRsvpIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_confirmed))
                    }
                    "declined" -> {
                        chipRsvpStatus.text = "Declined"
                        chipRsvpStatus.setChipBackgroundColorResource(R.color.status_declined)
                        chipRsvpStatus.setTextColor(Color.WHITE)
                        ivRsvpIcon.setImageResource(R.drawable.ic_rsvp_declined)
                        ivRsvpIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_declined))
                    }
                    else -> {
                        chipRsvpStatus.text = "Pending"
                        chipRsvpStatus.setChipBackgroundColorResource(R.color.status_pending)
                        chipRsvpStatus.setTextColor(Color.BLACK)
                        ivRsvpIcon.setImageResource(R.drawable.ic_rsvp_pending)
                        ivRsvpIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.status_pending))
                    }
                }
            }
        }

        private fun updateInvitationStatus(guest: Guest) {
            binding.apply {
                if (guest.invitationSent) {
                    tvInvitationStatus.text = "Invitation Sent"
                    tvInvitationStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.status_confirmed)
                    )
                    ivInvitationIcon.setImageResource(R.drawable.ic_email_sent)
                } else {
                    tvInvitationStatus.text = "Not Sent"
                    tvInvitationStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.status_pending)
                    )
                    ivInvitationIcon.setImageResource(R.drawable.ic_email_pending)
                }
            }
        }

        private fun showContextMenu(guest: Guest) {
            // TODO: Implement context menu with quick actions
            // - Call guest
            // - Send message
            // - Send invitation
            // - Mark as VIP
            // - Add to chat room
        }
    }
}
