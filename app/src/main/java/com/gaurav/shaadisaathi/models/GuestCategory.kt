package com.gaurav.shaadisaathi.models

data class GuestCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val guestCount: Int = 0,
    val color: String = "#2196F3",
    val isDefault: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", 0, "#2196F3", true)

    /**
     * Get category color as integer
     */
    fun getColorInt(): Int {
        return try {
            android.graphics.Color.parseColor(color)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#2196F3")
        }
    }

    /**
     * Get formatted guest count text
     */
    fun getGuestCountText(): String {
        return when (guestCount) {
            0 -> "No guests"
            1 -> "1 guest"
            else -> "$guestCount guests"
        }
    }

    /**
     * Check if category has guests
     */
    fun hasGuests(): Boolean = guestCount > 0

    /**
     * Get category icon resource name
     */
    fun getIconResourceName(): String {
        return when (icon) {
            "ic_family" -> "ic_family"
            "ic_friends" -> "ic_friends"
            "ic_colleagues" -> "ic_colleagues"
            else -> "ic_guest"
        }
    }

    companion object {
        /**
         * Get default categories for wedding guests
         */
        fun getDefaultCategories(): List<GuestCategory> {
            return listOf(
                GuestCategory(
                    id = "family",
                    name = "Family",
                    description = "Close family members and relatives",
                    icon = "ic_family",
                    color = "#E91E63",
                    isDefault = true
                ),
                GuestCategory(
                    id = "friends",
                    name = "Friends",
                    description = "Personal friends and close companions",
                    icon = "ic_friends",
                    color = "#2196F3",
                    isDefault = true
                ),
                GuestCategory(
                    id = "colleagues",
                    name = "Colleagues",
                    description = "Work colleagues and professional contacts",
                    icon = "ic_colleagues",
                    color = "#FF9800",
                    isDefault = true
                ),
                GuestCategory(
                    id = "others",
                    name = "Others",
                    description = "Other important guests and acquaintances",
                    icon = "ic_guest",
                    color = "#9C27B0",
                    isDefault = true
                )
            )
        }

        /**
         * Get category by ID
         */
        fun getCategoryById(id: String): GuestCategory? {
            return getDefaultCategories().find { it.id == id }
        }

        /**
         * Get category display name by ID
         */
        fun getCategoryDisplayName(id: String): String {
            return getCategoryById(id)?.name ?: id.replaceFirstChar { it.uppercase() }
        }

        /**
         * Get category color by ID
         */
        fun getCategoryColor(id: String): String {
            return getCategoryById(id)?.color ?: "#2196F3"
        }

        /**
         * Validate category data
         */
        fun isValidCategory(category: GuestCategory): Boolean {
            return category.id.isNotEmpty() &&
                    category.name.isNotEmpty() &&
                    category.description.isNotEmpty() &&
                    category.color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
        }

        /**
         * Create custom category
         */
        fun createCustomCategory(
            name: String,
            description: String,
            color: String = "#2196F3"
        ): GuestCategory {
            return GuestCategory(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                description = description,
                icon = "ic_guest",
                color = color,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
