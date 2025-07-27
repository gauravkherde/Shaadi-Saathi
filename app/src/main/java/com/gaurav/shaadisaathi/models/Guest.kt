package com.gaurav.shaadisaathi.models

data class Guest(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "", // family, friends, colleagues, vips
    val dietaryRestrictions: String = "",
    val plusOne: Boolean = false,
    val hostId: String = "", // ID of the host who added this guest
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", false, "", 0L)
}
