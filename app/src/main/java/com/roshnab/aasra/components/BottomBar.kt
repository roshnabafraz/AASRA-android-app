package com.roshnab.aasra.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Define Navigation Items
enum class BottomNavScreen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Donations("Donations", Icons.Filled.Favorite),
    Safety("Safety", Icons.Filled.Security),
    Profile("Profile", Icons.Filled.Person)
}

@Composable
fun AasraBottomBar(
    currentScreen: BottomNavScreen,
    onScreenSelected: (BottomNavScreen) -> Unit
) {
    // 1. Outer Container (The Main Bar)
    Surface(
        modifier = Modifier
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            .height(80.dp) // Total Height
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp), // Side padding for the whole row
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes items to fill space
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavScreen.values().forEach { screen ->
                FluidBottomNavItem(
                    screen = screen,
                    isSelected = screen == currentScreen,
                    onClick = { onScreenSelected(screen) }
                )
            }
        }
    }
}

@Composable
fun FluidBottomNavItem(
    screen: BottomNavScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 2. Smooth Color Transitions
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 3. The Inner Pill (Selection)
    Box(
        modifier = Modifier
            .height(56.dp) // Fixed height to match concentric spacing (80dp - 56dp = 24dp / 2 = 12dp padding)
            .clip(CircleShape) // Ensures the inner pill matches the outer curve
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .animateContentSize( // Smoothly animates width expansion
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp), // Inner padding for text/icon
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = screen.label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}