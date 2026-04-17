package com.roshnab.aasra.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Login Failed")
            }
        }
    }

    // SIGN UP
    fun signUp(
        email: String, pass: String, name: String, phone: String,
        role: String, skills: String, cnic: String, age: String, photoBase64: String?,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = authResult.user
                if (user != null) {
                    val userData = mutableMapOf<String, Any>(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to role,
                        "skills" to skills,
                        "cnic" to cnic,
                        "age" to age,
                        "createdAt" to System.currentTimeMillis()
                    )
                    if (!photoBase64.isNullOrBlank()) {
                        userData["photoUrl"] = "data:image/jpeg;base64,$photoBase64"
                    }
                    db.collection("users").document(user.uid).set(userData).await()
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Signup Failed")
            }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onNeedsProfile: (name: String, email: String, photoUrl: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    val doc = db.collection("users").document(user.uid).get().await()

                    if (doc.exists() && !doc.getString("cnic").isNullOrBlank()) {
                        onSuccess()
                    } else {
                        onNeedsProfile(
                            user.displayName ?: "",
                            user.email ?: "",
                            user.photoUrl?.toString() ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign In Error", e)
                onError(e.message ?: "Google Sign In Failed")
            }
        }
    }

    fun completeGoogleProfile(
        name: String, email: String, phone: String,
        role: String, skills: String, cnic: String, age: String, photoBase64: String?, photoUrlFromGoogle: String?,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    val userData = mutableMapOf<String, Any>(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to role,
                        "skills" to skills,
                        "cnic" to cnic,
                        "age" to age,
                        "createdAt" to System.currentTimeMillis()
                    )
                    if (!photoBase64.isNullOrBlank()) {
                        userData["photoUrl"] = "data:image/jpeg;base64,$photoBase64"
                    } else if (!photoUrlFromGoogle.isNullOrBlank()) {
                        userData["photoUrl"] = photoUrlFromGoogle
                    }
                    db.collection("users").document(user.uid).set(userData).await()
                    onSuccess()
                } else {
                    onError("No user logged in")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Profile completion failed")
            }
        }
    }
}