package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemEventBinding
import com.gaurav.shaadisaathi.models.Event

class EventAdapter(
    private val events: List<Event>,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvEventName.text = event.name
            binding.tvEventType.text = event.type.capitalize()
            binding.tvEventDate.text = "${event.date} at ${event.time}"
            binding.tvEventVenue.text = event.venue
            binding.tvEventDescription.text = event.description

            binding.root.setOnClickListener {
                onItemClick(event)
            }
        }
    }
}
