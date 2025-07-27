package com.gaurav.shaadisaathi.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.models.Guest
import kotlinx.coroutines.tasks.await

object InvitationManager {

    private const val TAG = "InvitationManager"
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sendDigitalInvitation(context: Context, guest: Guest, eventDetails: String): Result<Unit> {
        return try {
            when (guest.invitationPreference) {
                "digital" -> sendEmailInvitation(context, guest, eventDetails)
                "physical" -> Result.success(Unit) // Handle physical invitation separately
                "both" -> {
                    sendEmailInvitation(context, guest, eventDetails)
                    // Also mark for physical invitation
                }
                else -> sendEmailInvitation(context, guest, eventDetails)
            }

            // Update invitation status in Firestore
            updateInvitationStatus(guest.id)

            Log.d(TAG, "Invitation sent to ${guest.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending invitation", e)
            Result.failure(e)
        }
    }

    private fun sendEmailInvitation(context: Context, guest: Guest, eventDetails: String): Result<Unit> {
        return try {
            val subject = "Wedding Invitation - You're Invited!"
            val body = createInvitationEmailBody(guest, eventDetails)

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${guest.email}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            context.startActivity(Intent.createChooser(emailIntent, "Send Invitation"))
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending email invitation", e)
            Result.failure(e)
        }
    }

    private fun createInvitationEmailBody(guest: Guest, eventDetails: String): String {
        return """
            Dear ${guest.name},
            
            You are cordially invited to our wedding celebration!
            
            $eventDetails
            
            We would be honored by your presence on our special day.
            
            ${if (guest.hasPlusOne) "You are welcome to bring a plus one (${guest.plusOneName.ifEmpty { "guest" }})." else ""}
            
            Please RSVP by responding to this email or contacting us directly.
            
            Looking forward to celebrating with you!
            
            With love and excitement,
            The Wedding Couple
            
            ---
            This invitation was sent through ShaadiSaathi Wedding App
        """.trimIndent()
    }

    fun sendWhatsAppInvitation(context: Context, guest: Guest, message: String) {
        try {
            val phoneNumber = guest.phone.replace(Regex("[^+\\d]"), "")
            val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)

            Log.d(TAG, "WhatsApp invitation sent to ${guest.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp invitation", e)
        }
    }

    fun sendSMSInvitation(context: Context, guest: Guest, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${guest.phone}")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)

            Log.d(TAG, "SMS invitation sent to ${guest.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS invitation", e)
        }
    }

    private suspend fun updateInvitationStatus(guestId: String) {
        try {
            val updates = mapOf(
                "invitationSent" to true,
                "invitationSentAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("guests").document(guestId).update(updates).await()
            Log.d(TAG, "Invitation status updated for guest: $guestId")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating invitation status", e)
        }
    }

    suspend fun sendBulkInvitations(context: Context, guests: List<Guest>, eventDetails: String): Result<Int> {
        return try {
            var successCount = 0

            guests.forEach { guest ->
                if (!guest.invitationSent && guest.email.isNotEmpty()) {
                    val result = sendDigitalInvitation(context, guest, eventDetails)
                    if (result.isSuccess) {
                        successCount++
                    }
                }
            }

            Log.d(TAG, "Bulk invitations sent: $successCount out of ${guests.size}")
            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending bulk invitations", e)
            Result.failure(e)
        }
    }

    fun createInvitationTemplate(
        coupleName: String,
        eventDate: String,
        eventTime: String,
        venue: String,
        dressCode: String = ""
    ): String {
        return """
            🎉 WEDDING INVITATION 🎉
            
            $coupleName
            
            cordially invite you to celebrate their wedding
            
            📅 Date: $eventDate
            🕐 Time: $eventTime
            📍 Venue: $venue
            ${if (dressCode.isNotEmpty()) "👗 Dress Code: $dressCode" else ""}
            
            Your presence will make our day even more special!
            
            Please RSVP at your earliest convenience.
            
            #ShaadiSaathi #WeddingInvitation
        """.trimIndent()
    }
}
