package com.gaurav.shaadisaathi.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.gaurav.shaadisaathi.models.Guest
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CSVExporter {

    private const val TAG = "CSVExporter"

    fun exportGuestsToCSV(context: Context, guests: List<Guest>): Result<Uri> {
        return try {
            val fileName = "wedding_guests_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Name,Phone,Email,Category,RSVP Status,Meal Preference,Plus One,Plus One Name,Table Number,VIP,Special Requirements,Address,Notes,Created Date\n")

                // Write guest data
                guests.forEach { guest ->
                    writer.append("\"${guest.name}\",")
                    writer.append("\"${guest.phone}\",")
                    writer.append("\"${guest.email}\",")
                    writer.append("\"${guest.category}\",")
                    writer.append("\"${guest.rsvpStatus}\",")
                    writer.append("\"${guest.mealPreference}\",")
                    writer.append("\"${if (guest.hasPlusOne) "Yes" else "No"}\",")
                    writer.append("\"${guest.plusOneName}\",")
                    writer.append("\"${if (guest.tableNumber > 0) guest.tableNumber.toString() else ""}\",")
                    writer.append("\"${if (guest.isVip) "Yes" else "No"}\",")
                    writer.append("\"${guest.specialRequirements}\",")
                    writer.append("\"${guest.address}\",")
                    writer.append("\"${guest.notes}\",")
                    writer.append("\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(guest.createdAt))}\"\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            Log.d(TAG, "CSV exported successfully: $fileName")
            Result.success(uri)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV", e)
            Result.failure(e)
        }
    }

    fun shareCSV(context: Context, uri: Uri, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Wedding Guest List - $fileName")
            putExtra(Intent.EXTRA_TEXT, "Wedding guest list exported from ShaadiSaathi app")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Guest List"))
    }

    fun exportGuestStatisticsToCSV(context: Context, guests: List<Guest>): Result<Uri> {
        return try {
            val fileName = "guest_statistics_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                writer.append("Guest Statistics Report\n\n")

                // Overall Statistics
                writer.append("Category,Count\n")
                writer.append("Total Guests,${guests.size}\n")
                writer.append("Confirmed,${guests.count { it.rsvpStatus == "confirmed" }}\n")
                writer.append("Pending,${guests.count { it.rsvpStatus == "pending" }}\n")
                writer.append("Declined,${guests.count { it.rsvpStatus == "declined" }}\n")
                writer.append("Total Attending,${guests.filter { it.rsvpStatus == "confirmed" }.sumOf { it.getTotalGuests() }}\n")
                writer.append("VIP Guests,${guests.count { it.isVip }}\n")
                writer.append("Guests with Plus One,${guests.count { it.hasPlusOne }}\n\n")

                // Category Breakdown
                writer.append("Category Breakdown\n")
                writer.append("Category,Count\n")
                guests.groupBy { it.category }.forEach { (category, guestList) ->
                    writer.append("${category.replaceFirstChar { it.uppercase() }},${guestList.size}\n")
                }

                writer.append("\n")

                // Meal Preference Breakdown
                writer.append("Meal Preference Breakdown\n")
                writer.append("Preference,Count\n")
                guests.groupBy { it.mealPreference }.forEach { (preference, guestList) ->
                    writer.append("${preference.replaceFirstChar { it.uppercase() }},${guestList.size}\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            Log.d(TAG, "Statistics CSV exported successfully: $fileName")
            Result.success(uri)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting statistics CSV", e)
            Result.failure(e)
        }
    }
}
