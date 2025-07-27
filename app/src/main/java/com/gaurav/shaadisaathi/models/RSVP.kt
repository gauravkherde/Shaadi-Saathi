package com.gaurav.shaadisaathi.models

data class RSVP(
    val id: String = "",
    val eventId: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val status: String = "", // accepted, declined, maybe, pending
    val dietaryNotes: String = "",
    val plusOneAttending: Boolean = false,
    val respondedAt: Long = 0L
) {
    constructor() : this("", "", "", "", "", "", false, 0L)
}
