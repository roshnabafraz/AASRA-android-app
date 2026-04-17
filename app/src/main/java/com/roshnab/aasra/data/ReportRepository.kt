package com.roshnab.aasra.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    private fun triggerAiEngineNative(reportId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://aasra-ai-engine.onrender.com/process_report/$reportId")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Accept", "application/json")
                
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    Log.d("AI_ENGINE", "AI Engine triggered successfully. Code: $responseCode")
                } else {
                    Log.e("AI_ENGINE", "AI Engine failed. Code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AI_ENGINE", "Network error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun submitReport(report: Report): Boolean {
        return try {
            val docRef = reportsCollection.document()
            val finalReport = report.copy(reportId = docRef.id)
            docRef.set(finalReport).await()
            
            // Trigger AI Engine
            triggerAiEngineNative(docRef.id)
            
            // Trigger Notification to Volunteers
            NotificationService.broadcastToVolunteers(
                title = "New SOS Request",
                message = "A new ${report.category} emergency has been reported nearby by ${report.victimName}."
            )
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun acceptReport(reportId: String, volunteerId: String, volunteerName: String, volunteerPhone: String, victimId: String): Boolean {
        return try {
            reportsCollection.document(reportId)
                .update(
                    mapOf(
                        "status" to "accepted",
                        "volunteerId" to volunteerId,
                        "volunteerName" to volunteerName,
                        "volunteerPhone" to volunteerPhone
                    )
                ).await()
                
            // Notify the victim
            NotificationService.sendNotificationToUser(
                userId = victimId,
                title = "Help is on the way!",
                message = "A volunteer has accepted your request and is navigating to your location."
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteReport(reportId: String, reason: String): Boolean {
        return try {
            reportsCollection.document(reportId)
                .update(
                    mapOf(
                        "status" to "deleted",
                        "deleteReason" to reason
                    )
                ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun markAsResolved(reportId: String): Boolean {
        return try {
            reportsCollection.document(reportId)
                .update("status", "resolved")
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getOpenReportsFlow(): Flow<List<Report>> = callbackFlow {
        val query = reportsCollection
            .whereEqualTo("status", "pending")
            .limit(50)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_DEBUG", "Still failing: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val reports = snapshot.toObjects(Report::class.java)
                Log.d("FIRESTORE_DEBUG", "Success! Found ${reports.size} reports.")
                trySend(reports)
            } else {
                Log.d("FIRESTORE_DEBUG", "Snapshot was null")
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getMyAcceptedReportsFlow(volunteerId: String): Flow<List<Report>> = callbackFlow {
        val query = reportsCollection
            .whereEqualTo("status", "accepted")
            .whereEqualTo("volunteerId", volunteerId)
            .limit(50)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val reports = snapshot.toObjects(Report::class.java)
                trySend(reports)
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getMyActiveReportFlow(victimId: String): Flow<Report?> = callbackFlow {
        val query = reportsCollection
            .whereEqualTo("victimId", victimId)
            .whereIn("status", listOf("pending", "accepted"))
            .limit(1)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_DEBUG", "Error fetching my active report", error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val report = snapshot.documents.first().toObject(Report::class.java)
                trySend(report)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }
}