package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.activities.ContactItem
import com.gaurav.shaadisaathi.databinding.ItemContactImportBinding

class ContactImportAdapter(
    private var contacts: List<ContactItem>,
    private val onContactSelected: (ContactItem, Boolean) -> Unit
) : RecyclerView.Adapter<ContactImportAdapter.ContactViewHolder>() {

    private val selectedContacts = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactImportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(private val binding: ItemContactImportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: ContactItem) {
            binding.apply {
                // Contact name
                tvContactName.text = contact.name

                // Contact details
                tvContactPhone.text = contact.phone
                tvContactEmail.text = if (contact.email.isNotEmpty()) contact.email else "No email"

                // Contact initial
                val initial = if (contact.name.isNotEmpty()) {
                    contact.name.first().toString().uppercase()
                } else {
                    "C"
                }
                tvContactInitial.text = initial

                // Set background color for initial
                val colors = arrayOf(
                    R.color.colorPrimary,
                    R.color.category_family,
                    R.color.category_friends,
                    R.color.category_colleagues,
                    R.color.colorAccent
                )
                val colorIndex = contact.name.hashCode() % colors.size
                tvContactInitial.setBackgroundResource(colors[Math.abs(colorIndex)])

                // Email icon visibility
                ivEmailIcon.visibility = if (contact.email.isNotEmpty()) View.VISIBLE else View.GONE

                // Selection state
                val isSelected = selectedContacts.contains(getContactKey(contact))
                updateSelectionState(isSelected)

                // Click listeners
                root.setOnClickListener {
                    toggleSelection(contact)
                }

                checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != isSelected) {
                        toggleSelection(contact)
                    }
                }
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            binding.apply {
                checkboxSelect.isChecked = isSelected

                if (isSelected) {
                    root.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimaryLight)
                    )
                    root.alpha = 0.8f
                } else {
                    root.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.transparent)
                    )
                    root.alpha = 1.0f
                }
            }
        }

        private fun toggleSelection(contact: ContactItem) {
            val contactKey = getContactKey(contact)
            val wasSelected = selectedContacts.contains(contactKey)

            if (wasSelected) {
                selectedContacts.remove(contactKey)
            } else {
                selectedContacts.add(contactKey)
            }

            updateSelectionState(!wasSelected)
            onContactSelected(contact, !wasSelected)
        }
    }

    private fun getContactKey(contact: ContactItem): String {
        return "${contact.name}_${contact.phone}"
    }

    fun updateContacts(newContacts: List<ContactItem>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    fun getSelectedContacts(): List<ContactItem> {
        return contacts.filter { contact ->
            selectedContacts.contains(getContactKey(contact))
        }
    }

    fun selectAll() {
        selectedContacts.clear()
        contacts.forEach { contact ->
            selectedContacts.add(getContactKey(contact))
        }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedContacts.clear()
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int = selectedContacts.size

    fun isAllSelected(): Boolean = selectedContacts.size == contacts.size

    fun hasSelection(): Boolean = selectedContacts.isNotEmpty()
}
