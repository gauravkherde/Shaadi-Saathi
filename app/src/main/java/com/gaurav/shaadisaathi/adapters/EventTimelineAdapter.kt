package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemEventTimelineBinding
import com.gaurav.shaadisaathi.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventTimelineAdapter(
    private val events: MutableList<Event>,
    private val onEventClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : RecyclerView.Adapter<EventTimelineAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventTimelineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
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

    inner class EventViewHolder(private val binding: ItemEventTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventName.text = event.name
                tvEventType.text = event.getEventTypeDisplayName()
                tvVenueName.text = event.venue.name

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvEventDate.text = dateFormat.format(Date(event.date))

                tvEventTime.text = "${event.startTime} - ${event.endTime}"

                // Set event status color
                val statusColor = when {
                    event.isPast() -> ContextCompat.getColor(itemView.context, R.color.status_declined)
                    event.isToday() -> ContextCompat.getColor(itemView.context, R.color.status_confirmed)
                    else -> ContextCompat.getColor(itemView.context, R.color.status_pending)
                }
                viewStatusIndicator.setBackgroundColor(statusColor)

                // Click listeners
                root.setOnClickListener { onEventClick(event) }
                btnEditEvent.setOnClickListener { onEditClick(event) }
                btnDeleteEvent.setOnClickListener { onDeleteClick(event) }
            }
        }
    }
}
