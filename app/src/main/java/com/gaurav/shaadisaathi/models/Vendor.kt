package com.gaurav.shaadisaathi.models

data class Vendor(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val businessName: String = "",
    val category: String = "", // photographer, caterer, decorator, dj, makeup, etc.
    val subCategory: String = "",
    val description: String = "",
    val services: List<String> = emptyList(),
    val contactInfo: VendorContact = VendorContact(),
    val location: VendorLocation = VendorLocation(),
    val pricing: VendorPricing = VendorPricing(),
    val availability: VendorAvailability = VendorAvailability(),
    val portfolio: VendorPortfolio = VendorPortfolio(),
    val reviews: List<VendorReview> = emptyList(),
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val isVerified: Boolean = false,
    val isFavorite: Boolean = false,
    val isBooked: Boolean = false,
    val bookedEvents: List<String> = emptyList(), // Event IDs
    val contractSigned: Boolean = false,
    val contractUrl: String = "",
    val advancePaid: Boolean = false,
    val advanceAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "pending", // pending, advance_paid, fully_paid
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this(
        "", "", "", "", "", "", "", emptyList(), VendorContact(), VendorLocation(),
        VendorPricing(), VendorAvailability(), VendorPortfolio(), emptyList(), 0.0, 0,
        false, false, false, emptyList(), false, "", false, 0.0, 0.0, "pending",
        emptyList(), "", 0L, 0L
    )

    fun getDisplayName(): String = if (businessName.isNotEmpty()) businessName else name
    fun getCategoryDisplayName(): String = category.replaceFirstChar { it.uppercase() }
    fun getFormattedRating(): String = String.format("%.1f", rating)
    fun getStatusDisplayName(): String {
        return when {
            isBooked && contractSigned -> "Confirmed"
            isBooked -> "Booked"
            advancePaid -> "Advance Paid"
            else -> "Available"
        }
    }
}

data class VendorContact(
    val primaryPhone: String = "",
    val secondaryPhone: String = "",
    val email: String = "",
    val website: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val facebook: String = "",
    val contactPerson: String = "",
    val alternateContact: String = ""
) {
    constructor() : this("", "", "", "", "", "", "", "", "")
}

data class VendorLocation(
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val area: String = "",
    val landmark: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val serviceAreas: List<String> = emptyList(),
    val travelCharges: Double = 0.0,
    val maxTravelDistance: Int = 0 // in KM
) {
    constructor() : this("", "", "", "", "", "", 0.0, 0.0, emptyList(), 0.0, 0)

    fun getFullAddress(): String {
        return listOfNotNull(
            address.takeIf { it.isNotEmpty() },
            area.takeIf { it.isNotEmpty() },
            city.takeIf { it.isNotEmpty() },
            state.takeIf { it.isNotEmpty() },
            pincode.takeIf { it.isNotEmpty() }
        ).joinToString(", ")
    }
}

data class VendorPricing(
    val basePrice: Double = 0.0,
    val currency: String = "INR",
    val pricingType: String = "per_event", // per_event, per_hour, per_day, per_person
    val packages: List<VendorPackage> = emptyList(),
    val extraCharges: Map<String, Double> = emptyMap(),
    val discountOffered: Double = 0.0,
    val advancePercentage: Double = 30.0,
    val cancellationPolicy: String = "",
    val paymentTerms: String = ""
) {
    constructor() : this(0.0, "INR", "per_event", emptyList(), emptyMap(), 0.0, 30.0, "", "")
}

data class VendorPackage(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val duration: String = "",
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val isPopular: Boolean = false,
    val isCustomizable: Boolean = true
) {
    constructor() : this("", "", "", 0.0, "", emptyList(), emptyList(), false, true)
}

data class VendorAvailability(
    val availableDates: List<Long> = emptyList(),
    val unavailableDates: List<Long> = emptyList(),
    val workingDays: List<String> = emptyList(), // monday, tuesday, etc.
    val workingHours: String = "",
    val advanceBookingDays: Int = 30,
    val isAvailableWeekends: Boolean = true,
    val seasonalPricing: Boolean = false
) {
    constructor() : this(emptyList(), emptyList(), emptyList(), "", 30, true, false)
}

data class VendorPortfolio(
    val photos: List<String> = emptyList(), // Photo URLs or IDs
    val videos: List<String> = emptyList(),
    val certificates: List<String> = emptyList(),
    val awards: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val completedEvents: Int = 0,
    val specializations: List<String> = emptyList(),
    val equipmentOwned: List<String> = emptyList()
) {
    constructor() : this(emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, emptyList(), emptyList())
}

data class VendorReview(
    val id: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val eventId: String = "",
    val eventType: String = "",
    val reviewDate: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val photos: List<String> = emptyList(),
    val helpfulVotes: Int = 0,
    val response: String = "", // Vendor response
    val responseDate: Long = 0L
) {
    constructor() : this("", "", "", 0, "", "", "", 0L, false, emptyList(), 0, "", 0L)
}
