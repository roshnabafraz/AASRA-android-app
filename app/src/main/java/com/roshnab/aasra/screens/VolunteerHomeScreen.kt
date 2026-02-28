package com.roshnab.aasra.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roshnab.aasra.components.AasraBottomBar
import com.roshnab.aasra.components.AasraTopBar
import com.roshnab.aasra.components.BottomNavScreen
import com.roshnab.aasra.data.*
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import kotlinx.coroutines.tasks.await
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.regex.Pattern
import kotlin.math.*

@Composable
fun VolunteerHomeScreen(
    onLogoutClick: () -> Unit,
    onAddLocationClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onSupportClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentScreen by rememberSaveable { mutableStateOf(BottomNavScreen.Home) }

    // Map Data States
    var borderPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var riverPolygons by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }
    var riverBarrages by remember { mutableStateOf<List<Barrage>>(emptyList()) }
    var riverBasin by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    var selectedBarrage by remember { mutableStateOf<Barrage?>(null) }


    // Live Data
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val openReports by ReportRepository.getOpenReportsFlow().collectAsState(initial = emptyList<Report>())
    val myAcceptedReports by ReportRepository.getMyAcceptedReportsFlow(currentUserId).collectAsState(initial = emptyList<Report>())
    val activeReports = openReports + myAcceptedReports
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    // Location Tracking
    var myLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapController by remember { mutableStateOf<IMapController?>(null) }
    var hasZoomedToLocation by remember { mutableStateOf(false) } // To prevent constant re-zooming

    LaunchedEffect(Unit) {
        scope.launch {
            borderPoints = FloodRepository.fetchBorderData() // Pakistan Outline
            riverPolygons = RiverRepository.getRiverPolygons()
            riverBarrages = RiverRepository.getBarrages()
            riverBasin = RiverRepository.getRiverBasin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                BottomNavScreen.Home -> {
                    Scaffold(
                        topBar = { AasraTopBar(onProfileClick = { currentScreen = BottomNavScreen.Profile }, onNotificationClick = {}) }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    MapView(ctx).apply {
                                        setTileSource(TileSourceFactory.MAPNIK)
                                        setMultiTouchControls(true)
                                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                        mapController = this.controller
                                        mapController?.setZoom(6.0)
                                        mapController?.setCenter(GeoPoint(30.0, 70.0)) // Default start
                                    }
                                },
                                update = { map ->
                                    // 1. My Location & Auto Zoom
                                    if (map.overlays.none { it is MyLocationNewOverlay }) {
                                        val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                                        locOverlay.enableMyLocation()

                                        // ZOOM TO VOLUNTEER ON START
                                        locOverlay.runOnFirstFix {
                                            val loc = locOverlay.myLocation
                                            if (loc != null) {
                                                myLocation = loc
                                                if (!hasZoomedToLocation) {
                                                    scope.launch {
                                                        mapController?.animateTo(loc)
                                                        mapController?.setZoom(14.0) // Zoom level
                                                        hasZoomedToLocation = true
                                                    }
                                                }
                                            }
                                        }
                                        map.overlays.add(locOverlay)
                                    } else {
                                        val overlay = map.overlays.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay
                                        if (overlay?.myLocation != null) myLocation = overlay.myLocation
                                    }

                                    // 2. Pakistan Outline (Flood Zone)
                                    if (borderPoints.isNotEmpty()) {
                                        map.overlays.removeAll { it is Polygon && it.title == "Pakistan Flood Zone" }
                                        val pakistanShape = Polygon().apply {
                                            points = borderPoints
                                            fillPaint.color = android.graphics.Color.argb(20, 0, 100, 0) // Very light fill
                                            outlinePaint.color = android.graphics.Color.parseColor("#006400")
                                            outlinePaint.strokeWidth = 5f // Thicker border
                                            title = "Pakistan Flood Zone"
                                        }
                                        map.overlays.add(0, pakistanShape) // Add at bottom layer
                                    }

                                    // 3. River Basin
//                                    if (riverBasin.isNotEmpty()) {
//                                        map.overlays.removeAll { it is Polygon && it.title == "River Basin" }
//                                        val basinShape = Polygon().apply {
//                                            points = riverBasin
//                                            fillPaint.color = android.graphics.Color.argb(40, 135, 206, 235)
//                                            outlinePaint.color = android.graphics.Color.TRANSPARENT
//                                            title = "River Basin"
//                                        }
//                                        map.overlays.add(1, basinShape)
//                                    }

                                    // 4. River Water
//                                    if (riverPolygons.isNotEmpty()) {
//                                        map.overlays.removeAll { it is Polyline && it.title == "River Water" }
//                                        riverPolygons.forEach { riverPoints ->
//                                            val riverShape = Polyline().apply {
//                                                setPoints(riverPoints)
//                                                outlinePaint.color = android.graphics.Color.parseColor("#1E90FF")
//                                                outlinePaint.strokeWidth = 10f
//                                                title = "River Water"
//                                            }
//                                            map.overlays.add(riverShape)
//                                        }
//                                    }

                                    // 5. Green Dots (Barrages) - NOW WITH CLICK LISTENER
                                    if (riverBarrages.isNotEmpty()) {
                                        map.overlays.removeAll { it is Marker && it.title?.startsWith("Barrage") == true }

                                        val size = 32
                                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bitmap)
                                        val paint = Paint()
                                        paint.isAntiAlias = true
                                        paint.color = android.graphics.Color.parseColor("#008000")
                                        paint.style = Paint.Style.FILL
                                        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                                        paint.color = android.graphics.Color.WHITE
                                        paint.style = Paint.Style.STROKE
                                        paint.strokeWidth = 4f
                                        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2, paint)

                                        val dotIcon = BitmapDrawable(context.resources, bitmap)

                                        riverBarrages.forEach { barrage ->
                                            val marker = Marker(map).apply {
                                                position = barrage.location
                                                icon = dotIcon
                                                title = "Barrage: ${barrage.name}"
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                                                setOnMarkerClickListener { _, _ ->
                                                    selectedBarrage = barrage
                                                    true
                                                }
                                            }
                                            map.overlays.add(marker)
                                        }
                                    }


                                    if (activeReports.isNotEmpty()) {
                                        map.overlays.removeAll { it is Marker && it.title?.startsWith("SOS") == true }
                                        val rSize = 48
                                        val rBitmap = Bitmap.createBitmap(rSize, rSize, Bitmap.Config.ARGB_8888)
                                        val rCanvas = Canvas(rBitmap)
                                        val rPaint = Paint().apply {
                                            isAntiAlias = true; color = android.graphics.Color.RED; style = Paint.Style.FILL
                                        }
                                        rCanvas.drawCircle(rSize / 2f, rSize / 2f, rSize / 2f, rPaint)
                                        rPaint.color = android.graphics.Color.WHITE; rPaint.strokeWidth = 6f
                                        rCanvas.drawLine(rSize/2f, rSize/4f, rSize/2f, rSize/1.5f, rPaint)
                                        rCanvas.drawCircle(rSize/2f, rSize/1.25f, 3f, rPaint)
                                        val reportIcon = BitmapDrawable(context.resources, rBitmap)

                                        activeReports.forEach { report ->
                                            if (report.locationLat != 0.0 && report.locationLng != 0.0) {
                                                val marker = Marker(map).apply {
                                                    position = GeoPoint(report.locationLat, report.locationLng)
                                                    icon = reportIcon
                                                    title = "SOS: ${report.category}"
                                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                                    setOnMarkerClickListener { _, _ ->
                                                        selectedReport = report
                                                        true
                                                    }
                                                }
                                                map.overlays.add(marker)
                                            }
                                        }
                                    }
                                    map.invalidate()
                                }
                            )

                            if (selectedBarrage != null) {
                                BarrageDetailDialog(
                                    barrage = selectedBarrage!!,
                                    onDismiss = { selectedBarrage = null }
                                )
                            }

                            Box(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Volunteer Active") },
                                    leadingIcon = { Icon(Icons.Default.Warning, null) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }
                    }
                    if (selectedReport != null) {
                        FirebaseReportDialog(report = selectedReport!!, myLocation = myLocation, onDismiss = { selectedReport = null })
                    }
                }

                BottomNavScreen.Requests -> {
                    VolunteerRequestListScreen(volunteerLocation = myLocation)
                }

                BottomNavScreen.Notifications -> {
                    val notificationViewModel: com.roshnab.aasra.data.NotificationViewModel = viewModel()
                    com.roshnab.aasra.screens.NotificationScreen(
                        viewModel = notificationViewModel,
                        onBackClick = { currentScreen = BottomNavScreen.Home }
                    )
                }
                BottomNavScreen.Profile -> {
                    Box(modifier = Modifier.padding(bottom = 100.dp)) {
                        val profileViewModel: ProfileViewModel = viewModel()
                        ProfileScreen(
                            onBackClick = { currentScreen = BottomNavScreen.Home },
                            onLogoutClick = onLogoutClick,
                            onAddLocationClick = onAddLocationClick,
                            onEditProfileClick = onEditProfileClick,
                            isDarkTheme = isDarkTheme,
                            onThemeChanged = onThemeChanged,
                            onSupportClick = onSupportClick,
                            viewModel = profileViewModel
                        )
                    }
                }
                else -> {}
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AasraBottomBar(
                currentScreen = currentScreen,
                items = listOf(BottomNavScreen.Home, BottomNavScreen.Requests, BottomNavScreen.Notifications, BottomNavScreen.Profile),
                onScreenSelected = { screen -> currentScreen = screen }
            )
        }
    }
}

fun calculateDist(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
    val earthRadius = 6371.0
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

@Composable
fun FirebaseReportDialog(report: Report, myLocation: GeoPoint?, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val distanceText = if (myLocation != null && report.locationLat != 0.0) {
        val dist = calculateDist(myLocation.latitude, myLocation.longitude, report.locationLat, report.locationLng)
        if (dist < 1.0) "${String.format("%.0f", dist * 1000)}m away" else "${String.format("%.1f", dist)} km away"
    } else {
        "Distance Unknown"
    }

    val affectedMatcher = Pattern.compile("\\[Affected: (\\d+) people\\]").matcher(report.description)
    val affectedCount = if (affectedMatcher.find()) affectedMatcher.group(1) else "Unknown"

    val cleanDescription = report.description.replace("\\[Affected:.*?\\]".toRegex(), "").trim()
    val finalDescription = if (cleanDescription.isBlank()) "No additional details provided." else cleanDescription

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(24.dp)) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.category.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NearMe, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(text = distanceText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(text = "$affectedCount Affected", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Victim: ${report.victimName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Phone: ${report.victimPhone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(12.dp))

                Text("Situation:", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Text(finalDescription, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(24.dp))

                val scope = rememberCoroutineScope()
                val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }

                if (report.status == "accepted") {
                    Button(
                        onClick = {
                            scope.launch {
                                com.roshnab.aasra.data.ReportRepository.markAsResolved(report.reportId)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Solved", fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                val uid = auth.currentUser?.uid ?: return@launch
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val userDoc = db.collection("users").document(uid).get().await()
                                val vName = userDoc.getString("name") ?: "AASRA Volunteer"
                                val vPhone = userDoc.getString("phone") ?: ""
                                
                                com.roshnab.aasra.data.ReportRepository.acceptReport(report.reportId, uid, vName, vPhone, report.victimId)
                                launchGoogleMaps(context, report.locationLat, report.locationLng)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Accept & Navigate", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}