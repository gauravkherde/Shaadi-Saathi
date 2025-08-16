package com.gaurav.shaadisaathi.models

data class Guest(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "",
    val rsvpStatus: String = "pending",
    val mealPreference: String = "",
    val hasPlusOne: Boolean = false,
    val plusOneName: String = "",
    val plusOneConfirmed: Boolean = false,
    val address: String = "",
    val notes: String = "",
    val specialRequirements: String = "",
    val isVip: Boolean = false,
    val tableNumber: Int = 0,
    val relationToHost: String = "",
    val invitationSent: Boolean = false,
    val rsvpResponseAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // FIX: Add missing parameters
    val invitationPreference: String = "email", // email, sms, whatsapp, physical
    val languagePreference: String = "english", // english, hindi, regional
    val canUploadPhotos: Boolean = true
) {
    fun getTotalGuests(): Int = if (hasPlusOne && plusOneConfirmed) 2 else 1

    // FIX: Add missing helper methods
    fun getCategoryDisplayName(): String = category.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    fun getMealPreferenceDisplayName(): String = mealPreference.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}
