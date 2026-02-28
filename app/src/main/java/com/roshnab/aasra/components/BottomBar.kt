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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
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

enum class BottomNavScreen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Donations("Donations", Icons.Filled.Favorite),
    Requests("Requests", Icons.AutoMirrored.Filled.List), // <--- New Item
    Notifications("Notifications", Icons.Filled.Notifications),
    Profile("Profile", Icons.Filled.Person)
}

@Composable
fun AasraBottomBar(
    currentScreen: BottomNavScreen,
    items: List<BottomNavScreen>,
    onScreenSelected: (BottomNavScreen) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            .height(80.dp)
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
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
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