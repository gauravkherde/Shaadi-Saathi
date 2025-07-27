package com.gaurav.shaadisaathi.activities

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.adapters.ContactImportAdapter
import com.gaurav.shaadisaathi.databinding.ActivityImportContactsBinding
import com.gaurav.shaadisaathi.models.Guest
import java.util.*

data class ContactItem(
    val name: String,
    val phone: String,
    val email: String,
    var isSelected: Boolean = false
)

class ImportContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportContactsBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var contactAdapter: ContactImportAdapter
    private val contactList = mutableListOf<ContactItem>()
    private val selectedContacts = mutableListOf<ContactItem>()
    private val TAG = "ImportContactsActivity"

    private val contactsPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadContacts()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        checkContactsPermission()

        Log.d(TAG, "ImportContactsActivity initialized")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Import Contacts"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactImportAdapter(
            contacts = contactList,
            onContactSelected = { contact, isSelected ->
                contact.isSelected = isSelected
                if (isSelected) {
                    selectedContacts.add(contact)
                } else {
                    selectedContacts.remove(contact)
                }
                updateSelectedCount()
            },
            onSelectAll = { selectAll ->
                contactList.forEach { it.isSelected = selectAll }
                selectedContacts.clear()
                if (selectAll) {
                    selectedContacts.addAll(contactList)
                }
                contactAdapter.notifyDataSetChanged()
                updateSelectedCount()
            }
        )

        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(this@ImportContactsActivity)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnImportSelected.setOnClickListener {
            if (selectedContacts.isNotEmpty()) {
                showImportConfirmationDialog()
            } else {
                Toast.makeText(this, "Please select contacts to import", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectAll.setOnClickListener {
            val allSelected = contactList.all { it.isSelected }
            contactList.forEach { it.isSelected = !allSelected }
            selectedContacts.clear()
            if (!allSelected) {
                selectedContacts.addAll(contactList)
            }
            contactAdapter.notifyDataSetChanged()
            updateSelectedCount()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadContacts()
        }
    }

    private fun checkContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Contacts Permission Required")
            .setMessage("ShaadiSaathi needs access to your contacts to help you quickly add wedding guests.\n\nYour contact data is processed locally and not stored on our servers.")
            .setPositiveButton("Grant Permission") { _, _ ->
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Contacts permission is required to import contacts. Please enable it in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE

        Thread {
            try {
                val contacts = fetchContactsFromDevice()

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    contactList.clear()
                    contactList.addAll(contacts)
                    contactAdapter.notifyDataSetChanged()

                    if (contacts.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewContacts.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewContacts.visibility = View.VISIBLE
                        binding.tvContactCount.text = "${contacts.size} contacts found"
                    }

                    Log.d(TAG, "Loaded ${contacts.size} contacts")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading contacts", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@ImportContactsActivity, "Error loading contacts: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun fetchContactsFromDevice(): List<ContactItem> {
        val contacts = mutableListOf<ContactItem>()
        val contactsMap = mutableMapOf<String, ContactItem>()

        // Get contacts with names
        val nameCursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        nameCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: ""
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
                    if (cleanPhone.length >= 10) {
                        contactsMap[contactId] = ContactItem(name, cleanPhone, "")
                    }
                }
            }
        }

        // Get email addresses for existing contacts
        val emailCursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null,
            null,
            null
        )

        emailCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID))
                val email = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: ""

                contactsMap[contactId]?.let { contact ->
                    if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        contactsMap[contactId] = contact.copy(email = email)
                    }
                }
            }
        }

        contacts.addAll(contactsMap.values)
        return contacts.distinctBy { "${it.name}_${it.phone}" }
    }

    private fun updateSelectedCount() {
        val count = selectedContacts.size
        binding.btnImportSelected.text = if (count > 0) {
            "Import Selected ($count)"
        } else {
            "Import Selected"
        }

        binding.btnImportSelected.isEnabled = count > 0
        binding.tvSelectedCount.text = "$count selected"
    }

    private fun showImportConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Import Contacts")
            .setMessage("Import ${selectedContacts.size} selected contacts as wedding guests?\n\nThey will be added with 'Pending' RSVP status.")
            .setPositiveButton("Import") { _, _ ->
                importSelectedContacts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importSelectedContacts() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to import contacts", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnImportSelected.isEnabled = false
        binding.btnImportSelected.text = "Importing..."
        binding.progressBar.visibility = View.VISIBLE

        val batch = firestore.batch()
        var importedCount = 0
        val totalContacts = selectedContacts.size

        selectedContacts.forEach { contact ->
            val guestId = UUID.randomUUID().toString()

            val guest = Guest(
                id = guestId,
                hostId = currentUser.uid,
                name = contact.name,
                email = contact.email,
                phone = contact.phone,
                category = "friends", // Default category for imported contacts
                rsvpStatus = "pending",
                mealPreference = "veg",
                hasPlusOne = false,
                plusOneName = "",
                plusOneConfirmed = false,
                address = "",
                notes = "Imported from contacts",
                specialRequirements = "",
                isVip = false,
                tableNumber = 0,
                relationToHost = "",
                invitationPreference = "digital",
                languagePreference = "english",
                canUploadPhotos = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            batch.set(firestore.collection("guests").document(guestId), guest)
        }

        batch.commit()
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Successfully imported $totalContacts contacts")

                AlertDialog.Builder(this)
                    .setTitle("Import Successful")
                    .setMessage("Successfully imported $totalContacts contacts as wedding guests!\n\nYou can now manage their RSVP status and other details in the Guest List.")
                    .setPositiveButton("View Guest List") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("Import More", null)
                    .show()

                // Reset selection
                selectedContacts.clear()
                contactList.forEach { it.isSelected = false }
                contactAdapter.notifyDataSetChanged()
                updateSelectedCount()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnImportSelected.isEnabled = true
                binding.btnImportSelected.text = "Import Selected"

                Log.e(TAG, "Error importing contacts", e)
                Toast.makeText(this, "Error importing contacts: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
