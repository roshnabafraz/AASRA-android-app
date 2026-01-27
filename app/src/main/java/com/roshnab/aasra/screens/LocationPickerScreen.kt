package com.roshnab.aasra.screens

import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun LocationPickerScreen(
    onBackClick: () -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<org.osmdroid.api.IMapController?>(null) }
    var centerPoint by remember { mutableStateOf(GeoPoint(30.3753, 69.3451)) } // Default Pakistan Center

    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. THE MAP
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.WIKIMEDIA)
                    setMultiTouchControls(true)
                    controller.setZoom(5.0)
                    controller.setCenter(centerPoint)
                    mapController = controller

                    // Listener to track map movement
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            event?.source?.mapCenter?.let {
                                centerPoint = it as GeoPoint
                            }
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                    })
                }
            }
        )

        // 2. CENTER PIN (Static)
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Pin",
            modifier = Modifier.size(48.dp).align(Alignment.Center).padding(bottom = 24.dp), // Lift up slightly so tip is center
            tint = Color.Red
        )

        // 3. ACTIONS
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Icon(Icons.Filled.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }

            Button(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm Location")
            }
        }
    }

    // 4. NAME DIALOG
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Name this Location") },
            text = {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("e.g. Home, Office") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (locationName.isNotBlank()) {
                            onLocationSelected(locationName, centerPoint.latitude, centerPoint.longitude)
                            showDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}