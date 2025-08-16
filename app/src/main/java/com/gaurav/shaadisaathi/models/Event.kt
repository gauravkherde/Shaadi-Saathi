package com.gaurav.shaadisaathi.models

data class Event(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val venue: EventVenue = EventVenue(),
    val dresscode: String = "",
    val notes: String = "",
    val requirements: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getEventTypeDisplayName(): String = type.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    fun isPast(): Boolean = date < System.currentTimeMillis()
    fun isToday(): Boolean {
        val today = System.currentTimeMillis()
        val todayStart = today - (today % (24 * 60 * 60 * 1000))
        val todayEnd = todayStart + (24 * 60 * 60 * 1000)
        return date in todayStart..todayEnd
    }

    // Add this property for backward compatibility
    val time: String get() = "$startTime - $endTime"
}

data class EventVenue(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val capacity: Int = 0,
    val contactPerson: String = "",
    val contactPhone: String = ""
) {
    fun getFullAddress(): String = if (address.isNotEmpty()) "$address, $city, $state - $pincode" else "$city, $state"
}
