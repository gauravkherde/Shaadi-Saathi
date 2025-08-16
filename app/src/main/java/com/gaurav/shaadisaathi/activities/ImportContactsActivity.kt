package com.gaurav.shaadisaathi.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.ContactImportAdapter
import com.gaurav.shaadisaathi.adapters.ContactItem
import com.gaurav.shaadisaathi.databinding.ActivityImportContactsBinding
import com.gaurav.shaadisaathi.models.Guest
import com.gaurav.shaadisaathi.repository.GuestRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ImportContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportContactsBinding
    private lateinit var contactAdapter: ContactImportAdapter
    private val contactList = mutableListOf<ContactItem>()
    private val selectedContacts = mutableListOf<ContactItem>()
    private val guestRepository = GuestRepository()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val PERMISSION_REQUEST_READ_CONTACTS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        checkPermissionAndLoadContacts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Import Contacts"
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactImportAdapter(
            contacts = contactList,
            onContactToggle = { contact: ContactItem, isSelected: Boolean ->
                contact.isSelected = isSelected
                if (isSelected) {
                    selectedContacts.add(contact)
                } else {
                    selectedContacts.remove(contact)
                }
                updateSelectedCount()
            }
        )

        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(this@ImportContactsActivity)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectAll.setOnClickListener {
            selectAllContacts()
        }

        binding.btnDeselectAll.setOnClickListener {
            deselectAllContacts()
        }

        binding.btnImportSelected.setOnClickListener {
            importSelectedContacts()
        }
    }

    private fun checkPermissionAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_READ_CONTACTS
            )
        } else {
            loadContacts()
        }
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE

                val contacts = mutableListOf<ContactItem>()
                val cursor: Cursor? = contentResolver.query(
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

                cursor?.use {
                    val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val id = it.getString(idIndex)
                        val name = it.getString(nameIndex) ?: ""
                        val phone = it.getString(phoneIndex) ?: ""

                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            contacts.add(
                                ContactItem(
                                    id = id,
                                    name = name,
                                    phone = phone.replace(Regex("[^+\\d]"), ""), // Clean phone number
                                    email = ""
                                )
                            )
                        }
                    }
                }

                contactList.clear()
                contactList.addAll(contacts.distinctBy { it.phone }) // Remove duplicates
                contactAdapter.notifyDataSetChanged()

                binding.tvContactCount.text = "Found ${contactList.size} contacts"

            } catch (e: Exception) {
                Toast.makeText(this@ImportContactsActivity, "Error loading contacts: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun selectAllContacts() {
        contactList.forEach { it.isSelected = true }
        selectedContacts.clear()
        selectedContacts.addAll(contactList)
        contactAdapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    private fun deselectAllContacts() {
        contactList.forEach { it.isSelected = false }
        selectedContacts.clear()
        contactAdapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    private fun updateSelectedCount() {
        binding.tvSelectedCount.text = "Selected: ${selectedContacts.size}"
        binding.btnImportSelected.isEnabled = selectedContacts.isNotEmpty()
    }

    private fun importSelectedContacts() {
        if (selectedContacts.isEmpty()) {
            Toast.makeText(this, "Please select contacts to import", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@ImportContactsActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var successCount = 0
                for (contact in selectedContacts) {
                    val guest = Guest(
                        id = System.currentTimeMillis().toString() + contact.id,
                        hostId = currentUser.uid,
                        name = contact.name,
                        phone = contact.phone,
                        email = contact.email,
                        category = "friends", // Default category
                        rsvpStatus = "pending",
                        mealPreference = "",
                        hasPlusOne = false,
                        plusOneName = "",
                        plusOneConfirmed = false,
                        address = "",
                        notes = "Imported from contacts",
                        specialRequirements = "",
                        isVip = false,
                        tableNumber = 0,
                        relationToHost = "",
                        invitationSent = false,
                        rsvpResponseAt = 0L,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        invitationPreference = "email",
                        languagePreference = "english",
                        canUploadPhotos = true
                    )

                    val result = guestRepository.addGuest(guest)
                    if (result.isSuccess) {
                        successCount++
                    }
                }

                Toast.makeText(
                    this@ImportContactsActivity,
                    "Successfully imported $successCount out of ${selectedContacts.size} contacts",
                    Toast.LENGTH_LONG
                ).show()

                if (successCount > 0) {
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                Toast.makeText(this@ImportContactsActivity, "Error importing contacts: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts()
                } else {
                    Toast.makeText(this, "Permission denied. Cannot access contacts.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
