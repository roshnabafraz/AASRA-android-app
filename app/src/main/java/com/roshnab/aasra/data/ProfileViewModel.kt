package com.roshnab.aasra.data

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val totalDonated: Int = 0,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val safeLocations: List<SafeLocation> = emptyList()
)

data class EmergencyContact(
    val name: String = "",
    val number: String = ""
)

data class SafeLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val context = application.applicationContext

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                var name = user.displayName ?: "AASRA User"
                val email = user.email ?: ""
                var contacts = emptyList<EmergencyContact>()
                var locations = emptyList<SafeLocation>()

                try {
                    val snapshot = db.collection("users").document(user.uid).get().await()
                    if (snapshot.exists()) {
                        val fsName = snapshot.getString("name")
                        if (!fsName.isNullOrBlank()) name = fsName

                        val fsContacts = snapshot.get("emergencyContacts") as? List<Map<String, String>>
                        contacts = fsContacts?.map {
                            EmergencyContact(it["name"] ?: "", it["number"] ?: "")
                        } ?: emptyList()

                        val fsLocs = snapshot.get("safeLocations") as? List<Map<String, Any>>
                        locations = fsLocs?.map {
                            SafeLocation(
                                id = it["id"] as? String ?: "",
                                name = it["name"] as? String ?: "",
                                latitude = (it["latitude"] as? Double) ?: 0.0,
                                longitude = (it["longitude"] as? Double) ?: 0.0
                            )
                        } ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val allDonations = DonationRepository.fetchDonations()
                val userTotal = allDonations
                    .filter { it.name.equals(name, ignoreCase = true) }
                    .sumOf { it.amount }

                uiState = uiState.copy(
                    isLoading = false,
                    name = name,
                    email = email,
                    totalDonated = userTotal,
                    emergencyContacts = contacts,
                    safeLocations = locations
                )
            } else {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun addContactFromUri(contactUri: Uri) {
        viewModelScope.launch {
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                        var number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""

                        // Clean number
                        number = number.replace(" ", "").replace("-", "")

                        if (number.isNotBlank()) {
                            saveContactToFirestore(EmergencyContact(name, number))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveContactToFirestore(contact: EmergencyContact) {
        val user = auth.currentUser ?: return
        val currentList = uiState.emergencyContacts.toMutableList()
        if (currentList.any { it.number == contact.number }) return

        currentList.add(contact)
        uiState = uiState.copy(emergencyContacts = currentList)

        db.collection("users").document(user.uid)
            .update("emergencyContacts", currentList)
            .addOnFailureListener {
                val data = hashMapOf("emergencyContacts" to currentList)
                db.collection("users").document(user.uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    fun removeContact(contact: EmergencyContact) {
        val user = auth.currentUser ?: return
        val currentList = uiState.emergencyContacts.toMutableList()
        currentList.remove(contact)
        uiState = uiState.copy(emergencyContacts = currentList)
        db.collection("users").document(user.uid).update("emergencyContacts", currentList)
    }

    fun addSafeLocation(name: String, lat: Double, lng: Double) {
        val user = auth.currentUser ?: return
        val newLoc = SafeLocation(System.currentTimeMillis().toString(), name, lat, lng)
        val currentList = uiState.safeLocations.toMutableList()

        currentList.add(newLoc)
        uiState = uiState.copy(safeLocations = currentList)

        db.collection("users").document(user.uid)
            .update("safeLocations", currentList)
            .addOnFailureListener {
                val data = hashMapOf("safeLocations" to currentList)
                db.collection("users").document(user.uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    fun removeSafeLocation(location: SafeLocation) {
        val user = auth.currentUser ?: return
        val currentList = uiState.safeLocations.toMutableList()
        currentList.remove(location)
        uiState = uiState.copy(safeLocations = currentList)
        db.collection("users").document(user.uid).update("safeLocations", currentList)
    }
}