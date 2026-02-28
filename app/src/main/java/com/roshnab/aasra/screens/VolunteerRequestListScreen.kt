package com.roshnab.aasra.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roshnab.aasra.data.Report
import com.roshnab.aasra.data.ReportRepository
import com.roshnab.aasra.components.ShimmerCardItem
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import java.util.Date
import java.util.regex.Pattern
import kotlin.math.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerRequestListScreen(volunteerLocation: GeoPoint?) {
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val openReports by ReportRepository.getOpenReportsFlow().collectAsState(initial = emptyList<Report>())
    val myAcceptedReports by ReportRepository.getMyAcceptedReportsFlow(currentUserId).collectAsState(initial = emptyList<Report>())
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val activeReports = if (selectedTabIndex == 0) openReports else myAcceptedReports
    
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(selectedTabIndex) {
        isLoading = true
        delay(500) // Brief loading skeleton on tab switch
        isLoading = false
    }
    val sortedReports = remember(activeReports, volunteerLocation, selectedTabIndex) {
        if (volunteerLocation == null) {
            activeReports
        } else {
            activeReports.sortedBy { report: Report ->
                // Using the unique function for this screen
                listScreenCalculateDist(
                    volunteerLocation.latitude, volunteerLocation.longitude,
                    report.locationLat, report.locationLng
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Requests", fontWeight = FontWeight.Bold)
                            if (volunteerLocation != null && selectedTabIndex == 0) {
                                Text(
                                    "Sorted by nearest location",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Refresh logic */ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("New Urgents", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Accepted", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {

            if (volunteerLocation == null) {
                LocationWarningCard()
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(4) {
                        ShimmerCardItem()
                    }
                }
            } else if (sortedReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = Color.Green)
                        Spacer(Modifier.height(16.dp))
                        Text("No active SOS requests!", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom bar
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedReports) { report ->
                        ReportItemCard(report = report, volunteerLocation = volunteerLocation, isAccepted = selectedTabIndex == 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(report: Report, volunteerLocation: GeoPoint?, isAccepted: Boolean) {
    val context = LocalContext.current

    val distanceInfo = if (volunteerLocation != null && report.locationLat != 0.0) {
        val dist = listScreenCalculateDist(
            volunteerLocation.latitude, volunteerLocation.longitude,
            report.locationLat, report.locationLng
        )
        if (dist < 1.0) "${String.format("%.0f", dist * 1000)} m" else "${String.format("%.1f", dist)} km"
    } else {
        "-- km"
    }

    val timeAgo = getRelativeTime(report.timestamp)

    val affectedMatcher = Pattern.compile("\\[Affected: (\\d+) people\\]").matcher(report.description)
    val affectedCount = if (affectedMatcher.find()) affectedMatcher.group(1) else "1"

    val cleanDescription = report.description.replace("\\[Affected:.*?\\]".toRegex(), "").trim()
    val finalDescription = if (cleanDescription.isBlank()) "No details provided." else cleanDescription

    val categoryColor = when(report.category) {
        "Medical" -> MaterialTheme.colorScheme.error
        "Flood" -> Color(0xFF1E88E5)
        "Food" -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = categoryColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = report.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NearMe, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(text = distanceInfo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.width(8.dp))
                    Text("•", color = Color.Gray)
                    Spacer(Modifier.width(8.dp))

                    Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(text = timeAgo, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = report.victimName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = report.victimName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Affected Badge
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Group, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text(text = "$affectedCount Affected", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(text = finalDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // --- FOOTER ---
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${report.victimPhone}") }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.width(12.dp))

                val scope = rememberCoroutineScope()
                val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }

                if (!isAccepted) {
                    Button(
                        onClick = { 
                            scope.launch {
                                val uid = auth.currentUser?.uid ?: return@launch
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val userDoc = db.collection("users").document(uid).get().await()
                                val vName = userDoc.getString("name") ?: "AASRA Volunteer"
                                val vPhone = userDoc.getString("phone") ?: ""
                                
                                ReportRepository.acceptReport(report.reportId, uid, vName, vPhone, report.victimId)
                                launchGoogleMaps(context, report.locationLat, report.locationLng)
                            }
                        },
                        modifier = Modifier.weight(3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Accept & Navigate")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = { 
                            scope.launch {
                                ReportRepository.markAsResolved(report.reportId)
                            }
                        },
                        modifier = Modifier.weight(3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Mark as Solved")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// --- Unique Helper Functions for this Screen ---

private fun listScreenCalculateDist(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
    val earthRadius = 6371.0
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

private fun getRelativeTime(date: Date?): String {
    if (date == null) return "Just now"
    val now = System.currentTimeMillis()
    val time = date.time
    val diff = now - time
    return when {
        diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
        diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
        diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h ago"
        else -> "${diff / DateUtils.DAY_IN_MILLIS}d ago"
    }
}

@Composable
fun LocationWarningCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Location Unavailable",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Distances cannot be calculated. Please check map.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

fun launchGoogleMaps(context: Context, lat: Double, lng: Double) {
    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")

    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
        }
    }
}