package com.roshnab.aasra.screens

import android.Manifest
import android.graphics.Color
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.roshnab.aasra.components.AasraBottomBar
import com.roshnab.aasra.components.AasraTopBar
import com.roshnab.aasra.components.BottomNavScreen
import com.roshnab.aasra.data.FloodRepository
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun HomeScreen(
    onReportClick: (Double, Double) -> Unit,
    onDonationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(BottomNavScreen.Home) }

    var borderPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var mapController by remember { mutableStateOf<IMapController?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        scope.launch {
            borderPoints = FloodRepository.fetchBorderData()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // LAYER 1: MAP
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
                }
            },
            update = { map ->
                if (map.overlays.none { it is MyLocationNewOverlay }) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    map.overlays.add(locationOverlay)
                    myLocationOverlay = locationOverlay
                }

                if (borderPoints.isNotEmpty()) {
                    val oldPolys = map.overlays.filterIsInstance<Polygon>()
                    map.overlays.removeAll(oldPolys)

                    val pakistanShape = Polygon().apply {
                        points = borderPoints
                        fillPaint.color = Color.argb(70, 0, 100, 0)
                        outlinePaint.color = Color.parseColor("#006400")
                        outlinePaint.strokeWidth = 5f
                        title = "Pakistan Flood Zone"
                    }
                    map.overlays.add(pakistanShape)

                    if (map.zoomLevelDouble < 5) {
                        map.controller.setZoom(6.0)
                        map.controller.setCenter(GeoPoint(30.0, 70.0))
                    }
                }
                map.invalidate()
            }
        )

        AasraTopBar(
            onProfileClick = onProfileClick,
            onNotificationClick = { /* Future: Notification Screen */ }
        )

        SmallFloatingActionButton(
            onClick = {
                val location = myLocationOverlay?.myLocation
                if (location != null && mapController != null) {
                    mapController?.animateTo(location)
                    mapController?.setZoom(14.0)
                } else {
                    Toast.makeText(context, "Waiting for GPS...", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Locate Me")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    val loc = myLocationOverlay?.myLocation
                    if (loc != null) {
                        onReportClick(loc.latitude, loc.longitude)
                    } else {
                        Toast.makeText(context, "GPS Signal Lost", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text("Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            AasraBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    when (screen) {
                        BottomNavScreen.Donations -> {
                            onDonationClick()
                        }
                        BottomNavScreen.Profile -> {
                            onProfileClick()
                        }
                        else -> {
                            currentScreen = screen
                        }
                    }
                }
            )
        }
    }
}