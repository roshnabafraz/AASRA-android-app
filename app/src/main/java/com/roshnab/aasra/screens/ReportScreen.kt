package com.roshnab.aasra.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.roshnab.aasra.data.Report
import com.roshnab.aasra.data.ReportRepository
import com.roshnab.aasra.data.RiverRepository
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    latitude: Double,
    longitude: Double,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var description by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var affectedCount by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf("Medical") }
    var contactNumber by remember { mutableStateOf("") }
    var nearestBarrageInfo by remember { mutableStateOf("Calculating risk...") }

    var isSubmitting by remember { mutableStateOf(false) }

    // 1. Calculate Distance to Nearest Barrage (Danger Zone)
    LaunchedEffect(latitude, longitude) {
        val barrages = RiverRepository.getBarrages()
        var minDistance = Double.MAX_VALUE
        var nearestName = ""

        barrages.forEach { barrage ->
            // RENAMED FUNCTION CALL HERE TO FIX CONFLICT
            val dist = calculateRiskDistance(
                latitude, longitude,
                barrage.location.latitude, barrage.location.longitude
            )
            if (dist < minDistance) {
                minDistance = dist
                nearestName = barrage.name
            }
        }

        nearestBarrageInfo = if (minDistance < 1.0) {
            "${String.format("%.0f", minDistance * 1000)}m from $nearestName"
        } else {
            "${String.format("%.1f", minDistance)} km from $nearestName"
        }

        // Auto-fill phone number
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.phoneNumber != null) {
            contactNumber = user.phoneNumber ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Assistance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Location Card showing Distance to River
            LocationCard(latitude, longitude, nearestBarrageInfo)

            Text("What do you need?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IncidentTypeItem("Medical", Icons.Filled.MedicalServices, selectedType == "Medical") { selectedType = "Medical" }
                IncidentTypeItem("Food", Icons.Filled.Restaurant, selectedType == "Food") { selectedType = "Food" }
                IncidentTypeItem("Shelter", Icons.Filled.Home, selectedType == "Shelter") { selectedType = "Shelter" }
                IncidentTypeItem("Other", Icons.Filled.Info, selectedType == "Other") { selectedType = "Other" }
            }

            Text("Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            PeopleCounter(count = affectedCount, onCountChange = { affectedCount = it })

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Victim Age (Approx)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Person, null) }
            )

//            OutlinedButton(
//                onClick = { /* TODO: Open Camera Logic */ },
//                modifier = Modifier.fillMaxWidth().height(56.dp),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Icon(Icons.Outlined.PhotoCamera, null)
//                Spacer(Modifier.width(8.dp))
//                Text("Add Photo / Video Evidence")
//            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Additional Details (Optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Phone, null) }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (age.isBlank() || contactNumber.isBlank()) {
                        Toast.makeText(context, "Please enter Age and Contact Number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true

                    scope.launch {
                        val user = FirebaseAuth.getInstance().currentUser

                        val finalDesc = "$description\n[Affected: $affectedCount people]\n[Near: $nearestBarrageInfo]"

                        val report = Report(
                            victimId = user?.uid ?: "",
                            victimName = user?.displayName ?: "Anonymous",
                            victimPhone = contactNumber,
                            victimAge = age,
                            category = selectedType,
                            description = finalDesc,
                            locationLat = latitude,
                            locationLng = longitude,
                            status = "pending"
                        )

                        val success = ReportRepository.submitReport(report)
                        isSubmitting = false

                        if (success) {
                            Toast.makeText(context, "Request Sent Successfully!", Toast.LENGTH_LONG).show()
                            onSubmitClick()
                        } else {
                            Toast.makeText(context, "Failed to send. Check internet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SENDING SOS...", fontWeight = FontWeight.Bold)
                } else {
                    Text("SEND SOS REQUEST", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// --- RENAMED Helper Function to Avoid Conflict ---
private fun calculateRiskDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
    val earthRadius = 6371.0 // Radius in KM
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

@Composable
fun LocationCard(lat: Double, lng: Double, nearestInfo: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Selected Location", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("${String.format("%.5f", lat)}, ${String.format("%.5f", lng)}", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Water, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Proximity to Risk", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(nearestInfo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun IncidentTypeItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = name, tint = iconColor, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun PeopleCounter(count: Int, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (count > 1) onCountChange(count - 1) }) {
            Icon(Icons.Filled.Remove, null)
        }
        Text("$count Person(s) Affected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(onClick = { onCountChange(count + 1) }) {
            Icon(Icons.Filled.Add, null)
        }
    }
}