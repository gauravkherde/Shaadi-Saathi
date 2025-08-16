package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemEventBinding
import com.gaurav.shaadisaathi.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val events: MutableList<Event>, // FIX: Use MutableList for updates
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventName.text = event.name
                tvEventType.text = event.getEventTypeDisplayName()
                tvEventVenue.text = event.venue.name

                // FIX: Combine date and time into one field
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(event.date))

                // If you have tvEventDateTime, use it instead of separate fields
                tvEventDateTime.text = "$formattedDate at ${event.startTime}"

                // OR if you have separate fields, uncomment these:
                // tvEventDate.text = formattedDate
                // tvEventTime.text = event.startTime

                root.setOnClickListener { onEventClick(event) }
            }
        }
    }
}
