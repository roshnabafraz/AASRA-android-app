package com.roshnab.aasra.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val role: String = "victim",
    val totalDonated: Int = 0,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val safeLocations: List<SafeLocation> = emptyList(),
    val areNotificationsEnabled: Boolean = true,
    val photoUrl: String = ""
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
                var photoUrl = ""
                var role = "victim" // Default
                var contacts = emptyList<EmergencyContact>()
                var locations = emptyList<SafeLocation>()
                var notifPref = true

                try {
                    val snapshot = db.collection("users").document(user.uid).get().await()
                    if (snapshot.exists()) {
                        val fsName = snapshot.getString("name")
                        if (!fsName.isNullOrBlank()) name = fsName

                        val fsRole = snapshot.getString("role")
                        if (!fsRole.isNullOrBlank()) role = fsRole

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

                        notifPref = snapshot.getBoolean("notificationsEnabled") ?: true

                        val fsPhotoUrl = snapshot.getString("photoUrl")
                        if (!fsPhotoUrl.isNullOrBlank()) photoUrl = fsPhotoUrl
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val allDonations = DonationRepository.fetchDonations()
                val userTotal = allDonations
                    .filter { it.email.equals(email, ignoreCase = true) }
                    .sumOf { it.amount }

                uiState = uiState.copy(
                    isLoading = false,
                    name = name,
                    email = email,
                    role = role,
                    totalDonated = userTotal,
                    emergencyContacts = contacts,
                    safeLocations = locations,
                    areNotificationsEnabled = notifPref,
                    photoUrl = photoUrl
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
                db.collection("users").document(user.uid).set(data, SetOptions.merge())
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
                db.collection("users").document(user.uid).set(data, SetOptions.merge())
            }
    }

    fun removeSafeLocation(location: SafeLocation) {
        val user = auth.currentUser ?: return
        val currentList = uiState.safeLocations.toMutableList()
        currentList.remove(location)
        uiState = uiState.copy(safeLocations = currentList)
        db.collection("users").document(user.uid).update("safeLocations", currentList)
    }

    fun updateUserName(newName: String, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(profileUpdates).await()

                db.collection("users").document(user.uid)
                    .update("name", newName)
                    .await()

                uiState = uiState.copy(name = newName)
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun updateUserPassword(newPass: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return

        user.updatePassword(newPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun updateNotificationPreference(isEnabled: Boolean) {
        val user = auth.currentUser ?: return

        uiState = uiState.copy(areNotificationsEnabled = isEnabled)

        db.collection("users").document(user.uid)
            .update("notificationsEnabled", isEnabled)
            .addOnFailureListener {
            }
    }

    fun uploadProfileImage(imageBytes: ByteArray, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(false, "User not authenticated")

        viewModelScope.launch {
            try {
                // Decode and resize to max 256x256 to keep Firestore document small
                val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val scaled = Bitmap.createScaledBitmap(original, 256, 256, true)

                val compressed = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, compressed)
                val base64String = Base64.encodeToString(compressed.toByteArray(), Base64.DEFAULT)
                val dataUri = "data:image/jpeg;base64,$base64String"

                // Save to Firestore
                db.collection("users").document(user.uid)
                    .set(mapOf("photoUrl" to dataUri), SetOptions.merge())
                    .await()

                uiState = uiState.copy(photoUrl = dataUri)
                onResult(true, "Profile Picture Updated!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error: ${e.message}")
            }
        }
    }
}