package com.gaurav.shaadisaathi.models

import java.util.*

data class Guest(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "others", // family, friends, colleagues, others
    val rsvpStatus: String = "pending", // pending, confirmed, declined
    val mealPreference: String = "vegetarian", // vegetarian, nonvegetarian, jain, vegan
    val hasPlusOne: Boolean = false,
    val plusOneName: String = "",
    val plusOneConfirmed: Boolean = false,
    val address: String = "",
    val notes: String = "",
    val specialRequirements: String = "",
    val isVip: Boolean = false,
    val tableNumber: Int = 0,
    val relationToHost: String = "",
    val invitationPreference: String = "digital", // digital, physical, both
    val languagePreference: String = "english", // english, hindi, regional
    val canUploadPhotos: Boolean = true,
    val invitationSent: Boolean = false,
    val invitationSentAt: Long = 0L,
    val rsvpResponseAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "others", "pending", "vegetarian")

    /**
     * Get display name for category
     */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "family" -> "Family"
            "friends" -> "Friends"
            "colleagues" -> "Colleagues"
            "others" -> "Others"
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Get display name for meal preference
     */
    fun getMealPreferenceDisplayName(): String {
        return when (mealPreference) {
            "vegetarian" -> "Vegetarian"
            "nonvegetarian" -> "Non-Vegetarian"
            "jain" -> "Jain"
            "vegan" -> "Vegan"
            else -> mealPreference.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Get total number of guests (including plus one)
     */
    fun getTotalGuests(): Int {
        return if (hasPlusOne && plusOneConfirmed) 2 else 1
    }

    /**
     * Check if guest has complete contact information
     */
    fun hasCompleteContactInfo(): Boolean {
        return name.isNotEmpty() && (phone.isNotEmpty() || email.isNotEmpty())
    }

    /**
     * Get guest status color resource
     */
    fun getStatusColor(): String {
        return when (rsvpStatus) {
            "confirmed" -> "#4CAF50"
            "declined" -> "#F44336"
            else -> "#FF9800"
        }
    }

    /**
     * Get category color resource
     */
    fun getCategoryColor(): String {
        return when (category) {
            "family" -> "#E91E63"
            "friends" -> "#2196F3"
            "colleagues" -> "#FF9800"
            else -> "#9C27B0"
        }
    }

    /**
     * Check if guest can be contacted
     */
    fun canBeContacted(): Boolean {
        return phone.isNotEmpty() || email.isNotEmpty()
    }

    /**
     * Get formatted guest summary
     */
    fun getGuestSummary(): String {
        val attendingText = when (rsvpStatus) {
            "confirmed" -> if (hasPlusOne && plusOneConfirmed) "2 attending" else "1 attending"
            "declined" -> "Not attending"
            else -> "Response pending"
        }

        return "$name - ${getCategoryDisplayName()} - $attendingText"
    }

    /**
     * Check if guest has special requirements
     */
    fun hasSpecialRequirements(): Boolean {
        return specialRequirements.isNotEmpty()
    }

    /**
     * Get days since guest was added
     */
    fun getDaysSinceAdded(): Long {
        val currentTime = System.currentTimeMillis()
        return (currentTime - createdAt) / (1000 * 60 * 60 * 24)
    }

    /**
     * Check if guest needs follow-up
     */
    fun needsFollowUp(): Boolean {
        return rsvpStatus == "pending" && getDaysSinceAdded() > 7 && !invitationSent
    }

    /**
     * Get invitation status display text
     */
    fun getInvitationStatusText(): String {
        return when {
            invitationSent -> "Invitation Sent"
            rsvpStatus == "confirmed" -> "RSVP Confirmed"
            rsvpStatus == "declined" -> "RSVP Declined"
            else -> "Invitation Pending"
        }
    }

    /**
     * Validate guest data
     */
    fun isValid(): Boolean {
        return name.isNotEmpty() &&
                hostId.isNotEmpty() &&
                (phone.isNotEmpty() || email.isNotEmpty()) &&
                category.isNotEmpty() &&
                rsvpStatus in listOf("pending", "confirmed", "declined")
    }

    /**
     * Get search keywords for this guest
     */
    fun getSearchKeywords(): List<String> {
        val keywords = mutableListOf<String>()
        keywords.add(name.lowercase())
        keywords.add(getCategoryDisplayName().lowercase())
        keywords.add(rsvpStatus)
        if (phone.isNotEmpty()) keywords.add(phone)
        if (email.isNotEmpty()) keywords.add(email.lowercase())
        if (relationToHost.isNotEmpty()) keywords.add(relationToHost.lowercase())
        if (plusOneName.isNotEmpty()) keywords.add(plusOneName.lowercase())
        return keywords
    }

    companion object {
        /**
         * Create a sample guest for testing
         */
        fun createSampleGuest(hostId: String, name: String): Guest {
            return Guest(
                id = UUID.randomUUID().toString(),
                hostId = hostId,
                name = name,
                category = "friends",
                rsvpStatus = "pending",
                mealPreference = "vegetarian",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        /**
         * Get all available categories
         */
        fun getAvailableCategories(): List<String> {
            return listOf("family", "friends", "colleagues", "others")
        }

        /**
         * Get all available RSVP statuses
         */
        fun getAvailableRSVPStatuses(): List<String> {
            return listOf("pending", "confirmed", "declined")
        }

        /**
         * Get all available meal preferences
         */
        fun getAvailableMealPreferences(): List<String> {
            return listOf("vegetarian", "nonvegetarian", "jain", "vegan")
        }
    }
}
