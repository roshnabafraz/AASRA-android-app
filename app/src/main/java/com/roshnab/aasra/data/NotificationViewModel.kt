package com.roshnab.aasra.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.roshnab.aasra.utils.LocalPushHelper

class NotificationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsCollection = db.collection("notifications")

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        listenForNotifications()
    }

    private fun listenForNotifications() {
        val currentUser = auth.currentUser ?: return
        val currentUserId = currentUser.uid

        // In Firestore, you can query by "in" array to get personal + broadcast notifications
        notificationsCollection
            .whereIn("targetUserId", listOf(currentUserId, "all_volunteers"))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationViewModel", "Error fetching notifications", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifList = snapshot.toObjects(AppNotification::class.java)
                    
                    val oneHourAgo = System.currentTimeMillis() - 3600_000L
                    val recentNotifs = notifList.filter { 
                        it.timestamp == null || it.timestamp!!.time > oneHourAgo
                    }
                    
                    // Fire local push for newly added notifications
                    val oldIds = _notifications.value.map { it.notificationId }.toSet()
                    val newNotifs = recentNotifs.filter { !oldIds.contains(it.notificationId) && !it.isRead }
                    
                    if (oldIds.isNotEmpty()) {
                        for (n in newNotifs) {
                            LocalPushHelper.showNotification(n.title, n.message)
                        }
                    }
                    
                    _notifications.value = recentNotifs
                }
            }

        // Periodic cleanup task to remove expired notifications dynamically while app is open
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L) // check every minute
                val oneHourAgo = System.currentTimeMillis() - 3600_000L
                val currentList = _notifications.value
                val filteredList = currentList.filter {
                    it.timestamp == null || it.timestamp!!.time > oneHourAgo
                }
                if (filteredList.size != currentList.size) {
                    _notifications.value = filteredList
                }
            }
        }
    }

    fun markAsRead(notification: AppNotification) {
        if (!notification.isRead && notification.notificationId.isNotEmpty()) {
            viewModelScope.launch {
                NotificationService.markAsRead(notification.notificationId)
            }
        }
    }
}
