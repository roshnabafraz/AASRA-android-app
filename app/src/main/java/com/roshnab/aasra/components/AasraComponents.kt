package com.roshnab.aasra.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roshnab.aasra.ui.theme.ForestGreen
import com.roshnab.aasra.ui.theme.MintAccent

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Returns a scale modifier that shrinks to [pressedScale] when the
 *  [interactionSource] registers a press, then springs back. */
@Composable
private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "press_scale"
    )
    return this.scale(scale)
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. AasraPrimaryButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pill-shaped solid button using the brand Forest Green.
 * Includes a tactile spring-scale feedback on press.
 */
@Composable
fun AasraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
            disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. AasraOutlinedButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pill-shaped transparent-background button with a Forest Green border.
 * Use for secondary actions.
 */
@Composable
fun AasraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        border = BorderStroke(1.5.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. AasraElevatedCard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Clean flat card with soft elevation shadow and 16.dp rounded corners.
 * No border. Optionally clickable with spring press scale.
 */
@Composable
fun AasraElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val cardModifier = if (onClick != null) {
        modifier.pressScale(interactionSource, pressedScale = 0.97f)
    } else {
        modifier
    }

    if (onClick != null) {
        ElevatedCard(
            modifier = cardModifier,
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor   = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
            onClick = onClick,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor   = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. AasraAnimatedListItem — animated list entry wrapper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wrap list items with this to get a smooth fade + slide-in on appearance.
 * Use [visible] tied to an index-based delay for staggered reveals.
 */
@Composable
fun AasraAnimatedListItem(
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetY = { 20 }
                )
    ) {
        content()
    }
}
