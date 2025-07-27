package com.gaurav.shaadisaathi.models

data class Event(
    val id: String = "",
    val name: String = "",
    val type: String = "", // mehendi, sangeet, wedding, reception
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val address: String = "",
    val hostId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "", "", "", "", 0L)
}
