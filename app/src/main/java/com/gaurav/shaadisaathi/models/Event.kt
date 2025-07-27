package com.gaurav.shaadisaathi.models

data class Event(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "", // mehendi, sangam, wedding, reception, engagement
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val venue: EventVenue = EventVenue(),
    val dresscode: String = "",
    val guestList: List<String> = emptyList(), // Guest IDs invited to this event
    val tasks: List<EventTask> = emptyList(),
    val budget: EventBudget = EventBudget(),
    val vendors: List<String> = emptyList(), // Vendor IDs associated with this event
    val photos: List<String> = emptyList(), // Photo IDs from this event
    val isActive: Boolean = true,
    val reminderSent: Boolean = false,
    val weatherAlert: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Countdown and status
    val isCompleted: Boolean = false,
    val completedAt: Long = 0L,
    val notes: String = "",
    val requirements: String = "",
    val specialInstructions: String = ""
) {
    constructor() : this(
        "", "", "", "", "", 0L, "", "", EventVenue(), "", emptyList(), emptyList(),
        EventBudget(), emptyList(), emptyList(), true, false, true, 0L, 0L,
        false, 0L, "", "", ""
    )

    fun getDaysUntilEvent(): Long {
        val currentTime = System.currentTimeMillis()
        val timeDiff = date - currentTime
        return timeDiff / (1000 * 60 * 60 * 24)
    }

    fun getEventTypeDisplayName(): String {
        return when (type.lowercase()) {
            "mehendi" -> "Mehendi Ceremony"
            "sangam" -> "Sangam Ceremony"
            "wedding" -> "Wedding Ceremony"
            "reception" -> "Reception"
            "engagement" -> "Engagement"
            else -> type.replaceFirstChar { it.uppercase() }
        }
    }

    fun isUpcoming(): Boolean = date > System.currentTimeMillis()
    fun isPast(): Boolean = date < System.currentTimeMillis()
    fun isToday(): Boolean {
        val today = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        return date in today..(today + dayInMillis)
    }
}

data class EventVenue(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val contactPerson: String = "",
    val contactPhone: String = "",
    val capacity: Int = 0,
    val amenities: List<String> = emptyList(),
    val parkingAvailable: Boolean = false,
    val acAvailable: Boolean = false,
    val cateringAllowed: Boolean = true,
    val decorationAllowed: Boolean = true
) {
    constructor() : this("", "", "", "", "", 0.0, 0.0, "", "", 0, emptyList(), false, false, true, true)

    fun getFullAddress(): String {
        return listOfNotNull(
            address.takeIf { it.isNotEmpty() },
            city.takeIf { it.isNotEmpty() },
            state.takeIf { it.isNotEmpty() },
            pincode.takeIf { it.isNotEmpty() }
        ).joinToString(", ")
    }
}

data class EventTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "", // User ID
    val assignedToName: String = "",
    val dueDate: Long = 0L,
    val priority: String = "medium", // low, medium, high, urgent
    val category: String = "", // decoration, catering, photography, etc.
    val isCompleted: Boolean = false,
    val completedAt: Long = 0L,
    val completedBy: String = "",
    val notes: String = "",
    val estimatedHours: Int = 0,
    val actualHours: Int = 0,
    val cost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0L, "medium", "", false, 0L, "", "", 0, 0, 0.0, 0L)

    fun isOverdue(): Boolean = !isCompleted && dueDate < System.currentTimeMillis()
    fun getDaysUntilDue(): Long {
        val timeDiff = dueDate - System.currentTimeMillis()
        return timeDiff / (1000 * 60 * 60 * 24)
    }
}

data class EventBudget(
    val totalBudget: Double = 0.0,
    val allocatedBudget: Double = 0.0,
    val spentAmount: Double = 0.0,
    val categories: Map<String, CategoryBudget> = emptyMap()
) {
    constructor() : this(0.0, 0.0, 0.0, emptyMap())

    fun getRemainingBudget(): Double = totalBudget - spentAmount
    fun getBudgetUtilization(): Double = if (totalBudget > 0) (spentAmount / totalBudget) * 100 else 0.0
    fun isOverBudget(): Boolean = spentAmount > totalBudget
}

data class CategoryBudget(
    val categoryName: String = "",
    val allocatedAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val items: List<BudgetItem> = emptyList()
) {
    constructor() : this("", 0.0, 0.0, emptyList())

    fun getRemainingAmount(): Double = allocatedAmount - spentAmount
    fun getUtilizationPercentage(): Double = if (allocatedAmount > 0) (spentAmount / allocatedAmount) * 100 else 0.0
}

data class BudgetItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val estimatedCost: Double = 0.0,
    val actualCost: Double = 0.0,
    val vendorId: String = "",
    val vendorName: String = "",
    val isPaid: Boolean = false,
    val paidDate: Long = 0L,
    val paymentMethod: String = "",
    val receiptUrl: String = ""
) {
    constructor() : this("", "", "", 0.0, 0.0, "", "", false, 0L, "", "")
}
