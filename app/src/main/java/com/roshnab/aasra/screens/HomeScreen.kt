package com.roshnab.aasra.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
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
import com.roshnab.aasra.data.Barrage
import com.roshnab.aasra.data.FloodRepository
import com.roshnab.aasra.data.ProfileViewModel
import com.roshnab.aasra.data.RiverRepository
import com.roshnab.aasra.data.ReportRepository
import com.roshnab.aasra.data.Report
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun HomeScreen(
    onReportClick: (Double, Double) -> Unit,
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

    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid

    // Active Reports
    val myActiveReport by ReportRepository.getMyActiveReportFlow(currentUserId ?: "").collectAsState(initial = null)
    var showMyRequestDialog by remember { mutableStateOf(false) }

    // Map Data State
    var borderPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var riverPolygons by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }
    var riverBarrages by remember { mutableStateOf<List<Barrage>>(emptyList()) }
    var riverBasin by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    var mapController by remember { mutableStateOf<IMapController?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    var selectedBarrage by remember { mutableStateOf<Barrage?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName

        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

        scope.launch {
            borderPoints = FloodRepository.fetchBorderData()
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
                    // --- MAP VIEW ---
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                isHorizontalMapRepetitionEnabled = false
                                isVerticalMapRepetitionEnabled = false
                                setScrollableAreaLimitDouble(org.osmdroid.util.BoundingBox(85.0, 180.0, -85.0, -180.0))

                                mapController = this.controller
                                mapController?.setZoom(6.0)
                                mapController?.setCenter(GeoPoint(30.0, 70.0))
                            }
                        },
                        update = { map ->
                            if (map.overlays.none { it is MyLocationNewOverlay }) {
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                                locationOverlay.enableMyLocation()
                                map.overlays.add(locationOverlay)
                                myLocationOverlay = locationOverlay
                            }

                            if (borderPoints.isNotEmpty()) {
                                map.overlays.removeAll { it is Polygon && it.title == "Pakistan Flood Zone" }
                                val pakistanShape = Polygon().apply {
                                    points = borderPoints
                                    fillPaint.color = android.graphics.Color.argb(40, 0, 100, 0)
                                    outlinePaint.color = android.graphics.Color.parseColor("#006400")
                                    outlinePaint.strokeWidth = 3f
                                    title = "Pakistan Flood Zone"
                                }
                                map.overlays.add(pakistanShape)
                            }

                            // 3. River Basin
//                            if (riverBasin.isNotEmpty()) {
//                                map.overlays.removeAll { it is Polygon && it.title == "River Basin" }
//                                val basinShape = Polygon().apply {
//                                    points = riverBasin
//                                    fillPaint.color = android.graphics.Color.argb(40, 135, 206, 235)
//                                    outlinePaint.color = android.graphics.Color.argb(100, 70, 130, 180)
//                                    outlinePaint.strokeWidth = 2f
//                                    title = "River Basin"
//                                }
//                                map.overlays.add(1, basinShape)
//                            }

                            // 4. Detailed River Polygons
//                            if (riverPolygons.isNotEmpty()) {
//                                map.overlays.removeAll { it is Polyline && it.title == "River Water" }
//                                riverPolygons.forEach { riverPoints ->
//                                    val riverShape = Polyline().apply {
//                                        setPoints(riverPoints)
//                                        outlinePaint.color = android.graphics.Color.parseColor("#1E90FF")
//                                        outlinePaint.strokeWidth = 10f
//                                        title = "River Water"
//                                    }
//                                    map.overlays.add(riverShape)
//                                }
//                            }

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

                            map.invalidate()
                        }
                    )

                    if (selectedBarrage != null) {
                        BarrageDetailDialog(
                            barrage = selectedBarrage!!,
                            onDismiss = { selectedBarrage = null }
                        )
                    }

                    Box(Modifier.align(Alignment.TopCenter)) {
                        AasraTopBar(onProfileClick = { currentScreen = BottomNavScreen.Profile }, onNotificationClick = { })
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            val loc = myLocationOverlay?.myLocation
                            if (loc != null) {
                                mapController?.animateTo(loc)
                                mapController?.setZoom(14.0)
                            } else {
                                Toast.makeText(context, "Locating...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Locate Me")
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            val loc = myLocationOverlay?.myLocation
                            if (loc != null) onReportClick(loc.latitude, loc.longitude)
                            else {
                                onReportClick(31.5204, 74.3587)
                                Toast.makeText(context, "Using Default Location", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).height(56.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (myActiveReport != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 100.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth()
                                .clickable { showMyRequestDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("SOS Request Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Tap to view details or cancel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    if (showMyRequestDialog && myActiveReport != null) {
                        VictimRequestDialog(
                            report = myActiveReport!!,
                            onDismiss = { showMyRequestDialog = false }
                        )
                    }

                }

                BottomNavScreen.Donations -> {
                    Box(modifier = Modifier.padding(bottom = 100.dp)) {
                        DonationScreen(onBackClick = { currentScreen = BottomNavScreen.Home })
                    }
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

        // Bottom Navigation Bar
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AasraBottomBar(
                currentScreen = currentScreen,
                items = listOf(BottomNavScreen.Home, BottomNavScreen.Donations, BottomNavScreen.Notifications, BottomNavScreen.Profile),
                onScreenSelected = { screen -> currentScreen = screen }
            )
        }
    }
}

@Composable
fun BarrageDetailDialog(barrage: Barrage, onDismiss: () -> Unit) {

    var liveStatus by remember { mutableStateOf("Fetching live data...") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(barrage) {
        scope.launch {
            liveStatus = RiverRepository.getLiveFloodRisk(
                barrage.location.latitude,
                barrage.location.longitude
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = barrage.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("River", barrage.river)
                DetailRow("Height", barrage.height)
                DetailRow("Live Status", liveStatus, isStatus = true)

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    FlowBox(
                        title = "Historic Inflow",
                        value = barrage.inflow,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE3F2FD),
                        textColor = Color(0xFF1565C0)
                    )
                    Spacer(Modifier.width(8.dp))
                    FlowBox(
                        title = "Historic Outflow",
                        value = barrage.outflow,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFFFEBEE),
                        textColor = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isStatus: Boolean = false) {
    val isWarning = isStatus && value.contains("WARNING", ignoreCase = true)
    val isHighRisk = isStatus && value.contains("HIGH RISK", ignoreCase = true)
    val isNormal = isStatus && value.contains("NORMAL", ignoreCase = true)

    val valueColor = when {
        isHighRisk -> Color.Red
        isWarning -> Color(0xFFF57F17) // Orange
        isNormal -> Color(0xFF2E7D32) // Green
        isStatus -> Color.Gray // Loading state
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun FlowBox(title: String, value: String, modifier: Modifier, color: Color, textColor: Color) {
    Column(
        modifier = modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.WaterDrop, null, tint = textColor, modifier = Modifier.size(20.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = textColor)
        Text(
            text = value.replace("(", "\n("), // Break line for trend
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun VictimRequestDialog(report: Report, onDismiss: () -> Unit) {
    var showCancelConfirm by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("Resolved") }
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(24.dp)) {
                if (!showCancelConfirm) {
                    if (report.status == "accepted") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("HELP IS ON ITS WAY!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Volunteer: ${report.volunteerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Phone: ${report.volunteerPhone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Active SOS Request",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Status: ${report.status.uppercase()}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Category: ${report.category}")
                    Spacer(Modifier.height(8.dp))
                    Text("Details: ${report.description}")

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { showCancelConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel Request", fontSize = 16.sp)
                    }
                } else {
                    Text(
                        text = "Cancel Request",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Please specify the reason for cancellation:")
                    
                    Spacer(Modifier.height(8.dp))
                    
                    val reasons = listOf("Resolved", "Accidental Trigger", "Help Arrived", "Other")
                    reasons.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 4.dp)) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(reason)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCancelConfirm = false }) {
                            Text("Back")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    ReportRepository.deleteReport(report.reportId, selectedReason)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Confirm Cancel")
                        }
                    }
                }
            }
        }
    }
}