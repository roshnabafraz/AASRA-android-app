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
import org.osmdroid.util.GeoPoint
import java.util.Date
import java.util.regex.Pattern
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerRequestListScreen(volunteerLocation: GeoPoint?) {
    // 1. Fetch Reports
    val activeReports by ReportRepository.getOpenReportsFlow().collectAsState(initial = emptyList<Report>())

    // 2. Sort by Distance (Nearest first)
    val sortedReports = remember(activeReports, volunteerLocation) {
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
            TopAppBar(
                title = {
                    Column {
                        Text("Urgent Requests", fontWeight = FontWeight.Bold)
                        if (volunteerLocation != null) {
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

            if (sortedReports.isEmpty()) {
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
                        ReportItemCard(report = report, volunteerLocation = volunteerLocation)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(report: Report, volunteerLocation: GeoPoint?) {
    val context = LocalContext.current

    // --- Calculations ---
    val distanceInfo = if (volunteerLocation != null && report.locationLat != 0.0) {
        // Using the unique function for this screen
        val dist = listScreenCalculateDist(
            volunteerLocation.latitude, volunteerLocation.longitude,
            report.locationLat, report.locationLng
        )
        if (dist < 1.0) "${String.format("%.0f", dist * 1000)} m" else "${String.format("%.1f", dist)} km"
    } else {
        "-- km"
    }

    // Time Ago
    val timeAgo = getRelativeTime(report.timestamp)

    // Affected Count Extraction
    val affectedMatcher = Pattern.compile("\\[Affected: (\\d+) people\\]").matcher(report.description)
    val affectedCount = if (affectedMatcher.find()) affectedMatcher.group(1) else "1"

    // Clean Description
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

            // --- HEADER ---
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

            // --- BODY ---
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

                Button(
                    onClick = { launchGoogleMaps(context, report.locationLat, report.locationLng) },
                    modifier = Modifier.weight(3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Accept & Navigate")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
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