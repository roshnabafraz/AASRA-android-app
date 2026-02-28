package com.roshnab.aasra.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AppNotification(
    val notificationId: String = "",
    val targetUserId: String = "", // specific UID or "all_volunteers"
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)
