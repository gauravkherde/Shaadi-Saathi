package com.gaurav.shaadisaathi.models

data class VendorLocation(
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun getFullAddress(): String {
        return buildString {
            if (address.isNotEmpty()) append(address)
            if (city.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(city)
            }
            if (state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(state)
            }
            if (pincode.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(pincode)
            }
        }.ifEmpty { "Address not specified" }
    }
}

data class VendorContactInfo(
    val primaryPhone: String = "",
    val secondaryPhone: String = "",
    val email: String = "",
    val website: String = ""
)

data class VendorPricing(
    val basePrice: Double = 0.0,
    val priceRange: String = "",
    val perHourRate: Double = 0.0,
    val currency: String = "INR"
)

data class Vendor(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val contactInfo: VendorContactInfo = VendorContactInfo(),
    val location: VendorLocation = VendorLocation(),
    val pricing: VendorPricing = VendorPricing(),
    val services: String = "",
    val specialization: String = "",
    val experience: String = "",
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isBooked: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = name.ifEmpty { "Unnamed Vendor" }

    fun getCategoryDisplayName(): String = category.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}
