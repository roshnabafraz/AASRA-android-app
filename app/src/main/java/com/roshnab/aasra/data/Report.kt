package com.roshnab.aasra.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Report(
    val reportId: String = "",
    val victimId: String = "",
    val victimName: String = "",
    val victimPhone: String = "",
    val victimAge: String = "",
    val category: String = "",
    val description: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val status: String = "pending",
    val imageUrl: String = "",
    val deleteReason: String = "",
    val volunteerId: String = "",
    val volunteerName: String = "",
    val volunteerPhone: String = "",
    @ServerTimestamp val timestamp: Date? = null
)