package com.roshnab.aasra.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationService {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    suspend fun sendNotificationToUser(userId: String, title: String, message: String): Boolean {
        return try {
            val docRef = notificationsCollection.document()
            val notification = AppNotification(
                notificationId = docRef.id,
                targetUserId = userId,
                title = title,
                message = message
            )
            docRef.set(notification).await()
            true
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to send notification to $userId", e)
            false
        }
    }

    suspend fun broadcastToVolunteers(title: String, message: String): Boolean {
        return sendNotificationToUser("all_volunteers", title, message)
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            notificationsCollection.document(notificationId).update("read", true).await()
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to mark notification $notificationId as read", e)
        }
    }
}
