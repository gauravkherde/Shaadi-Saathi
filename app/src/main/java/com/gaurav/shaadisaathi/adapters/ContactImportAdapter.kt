package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemContactImportBinding

data class ContactItem(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    var isSelected: Boolean = false
)

class ContactImportAdapter(
    private val contacts: MutableList<ContactItem>,
    private val onContactToggle: (ContactItem, Boolean) -> Unit // FIX: Use onContactToggle parameter name
) : RecyclerView.Adapter<ContactImportAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactImportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<ContactItem>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    fun getSelectedContacts(): List<ContactItem> {
        return contacts.filter { it.isSelected }
    }

    fun selectAll() {
        contacts.forEach { it.isSelected = true }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        contacts.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(private val binding: ItemContactImportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: ContactItem) {
            binding.apply {
                tvContactName.text = contact.name
                tvContactPhone.text = if (contact.phone.isNotEmpty()) contact.phone else "No phone"
                tvContactEmail.text = if (contact.email.isNotEmpty()) contact.email else "No email"

                // Set contact initial
                val initial = contact.name.firstOrNull()?.toString()?.uppercase() ?: "C"
                tvContactInitial.text = initial

                // Checkbox state
                checkboxSelect.isChecked = contact.isSelected
                checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                    contact.isSelected = isChecked
                    onContactToggle(contact, isChecked)
                }

                // Hide email if empty
                tvContactEmail.visibility = if (contact.email.isEmpty()) View.GONE else View.VISIBLE

                // Click listener for whole item
                root.setOnClickListener {
                    checkboxSelect.isChecked = !checkboxSelect.isChecked
                }
            }
        }
    }
}
