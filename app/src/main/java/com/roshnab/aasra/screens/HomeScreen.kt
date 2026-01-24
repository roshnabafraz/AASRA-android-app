package com.roshnab.aasra.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roshnab.aasra.components.AasraBottomBar
import com.roshnab.aasra.components.BottomNavScreen


@Composable
fun HomeScreen() {
    var currentScreen by remember { mutableStateOf(BottomNavScreen.Home) }

    // Using Box to stack layers (Map -> Report Button -> Bottom Bar)
    Box(modifier = Modifier.fillMaxSize()) {

        // Placeholder for Google Map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E0E0)) // Light Grey Map placeholder
        ) {
            Text(
                text = "Google Map Background",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
                        )
                    )
            )
        }

        when(currentScreen) {
            BottomNavScreen.Home -> { /* Show Home Overlay Elements */ }
            else -> { /* Other screens content */ }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Floating "Report" Pill
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Open Report Screen */ },
                // CircleShape creates the perfect "Pill" match with your bottom bar
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.error, // Keeps it Urgent (Red)
                contentColor = MaterialTheme.colorScheme.onError, // White text
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .height(56.dp) // Matches the height of your nav items
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Custom Pill Bottom Bar
            AasraBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { newScreen -> currentScreen = newScreen }
            )
        }
    }
}