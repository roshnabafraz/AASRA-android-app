package com.roshnab.aasra.screens

import com.roshnab.aasra.components.AasraBottomBar
import com.roshnab.aasra.components.BottomNavScreen
import android.Manifest
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MyLocation // New Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.roshnab.aasra.data.FloodData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(BottomNavScreen.Home) }

    // Data Loading
    val floodPoints = remember { FloodData.getFloodBoundary() }
    val riverPoints = remember { FloodData.getIndusRiver() }

    // Location State
    var mapController by remember { mutableStateOf<org.osmdroid.api.IMapController?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled silently */ }

    LaunchedEffect(Unit) {
        // Load OSM Config
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName

        // Request GPS
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- LAYER 1: OPEN STREET MAP ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                }
            },
            update = { map ->
                // 1. SETUP LOCATION OVERLAY (Blue Dot)
                // We check if it's already added to avoid duplicates
                if (map.overlays.none { it is MyLocationNewOverlay }) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    map.overlays.add(locationOverlay)
                    myLocationOverlay = locationOverlay
                }

                // 2. DRAW PAKISTAN HIGHLIGHT (Green)
                if (floodPoints.isNotEmpty()) {
                    // Remove old polygons to prevent "stacking" (which makes transparency turn solid)
                    val oldPolygons = map.overlays.filterIsInstance<Polygon>()
                    map.overlays.removeAll(oldPolygons)

                    val countryHighlight = Polygon().apply {
                        points = floodPoints

                        // FILL: Semi-Transparent Green (Active/Highlighted look)
                        // argb(50, 0, 100, 0) -> 50 is Transparency, 100 is Green intensity
                        fillPaint.color = android.graphics.Color.argb(50, 0, 100, 0)

                        // BORDER: Solid Dark Green
                        outlinePaint.color = android.graphics.Color.parseColor("#006400")
                        outlinePaint.strokeWidth = 5f

                        title = "Pakistan"
                    }
                    map.overlays.add(countryHighlight)

                    // Center Camera on Pakistan
                    // Only reset center if user hasn't zoomed in yet (optional check)
                    if (map.zoomLevelDouble < 5) {
                        map.controller.setZoom(6.0)
                        map.controller.setCenter(GeoPoint(30.0, 70.0))
                    }
                }

                map.invalidate() // Refresh map
            }
        )

        // --- 2. FLOATING "LOCATE ME" BUTTON (Top Right) ---
        SmallFloatingActionButton(
            onClick = {
                val location = myLocationOverlay?.myLocation
                if (location != null) {
                    mapController?.animateTo(location)
                    mapController?.setZoom(14.0)
                } else {
                    Toast.makeText(context, "Waiting for GPS... (Check Emulator settings)", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Locate Me")
        }

        // --- 3. BOTTOM UI (Report & Nav) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Report Button
            ExtendedFloatingActionButton(
                onClick = {
                    val userLoc = myLocationOverlay?.myLocation
                    if(userLoc != null) {
                        // TODO: Send userLoc.latitude & userLoc.longitude to Report Screen
                        Toast.makeText(context, "Location Captured: ${userLoc.latitude}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "GPS Signal Lost", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(bottom = 16.dp).height(56.dp)
            ) {
                Icon(Icons.Filled.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text("Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            AasraBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    }
}

//package com.roshnab.aasra.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Warning
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import com.roshnab.aasra.components.AasraBottomBar
//import com.roshnab.aasra.components.BottomNavScreen
//
//
//@Composable
//fun HomeScreen() {
//    var currentScreen by remember { mutableStateOf(BottomNavScreen.Home) }
//
//    // Using Box to stack layers (Map -> Report Button -> Bottom Bar)
//    Box(modifier = Modifier.fillMaxSize()) {
//
//        // Placeholder for Google Map
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color(0xFFE0E0E0)) // Light Grey Map placeholder
//        ) {
//            Text(
//                text = "Google Map Background",
//                color = Color.Gray,
//                modifier = Modifier.align(Alignment.Center)
//            )
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(150.dp)
//                    .align(Alignment.BottomCenter)
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
//                        )
//                    )
//            )
//        }
//
//        when(currentScreen) {
//            BottomNavScreen.Home -> { /* Show Home Overlay Elements */ }
//            else -> { /* Other screens content */ }
//        }
//
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            // Floating "Report" Pill
//            ExtendedFloatingActionButton(
//                onClick = { /* TODO: Open Report Screen */ },
//                shape = CircleShape,
//                containerColor = MaterialTheme.colorScheme.error,
//                contentColor = MaterialTheme.colorScheme.onError,
//                elevation = FloatingActionButtonDefaults.elevation(8.dp),
//                modifier = Modifier
//                    .padding(bottom = 16.dp)
//                    .height(56.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Filled.Warning,
//                    contentDescription = null,
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = "Report",
//                    style = MaterialTheme.typography.titleLarge,
//                    fontWeight = FontWeight.ExtraBold
//                )
//            }
//
//            // Custom Pill Bottom Bar
//            AasraBottomBar(
//                currentScreen = currentScreen,
//                onScreenSelected = { newScreen -> currentScreen = newScreen }
//            )
//        }
//    }
//}