package com.roshnab.aasra.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ───────────────────────────────────────────────────────────────────
val ForestGreen     = Color(0xFF0E4525)   // Primary — deep forest green
val ForestGreenDark = Color(0xFF092E19)   // Pressed / container state
val SageGreen       = Color(0xFFD4EAD9)   // Secondary — light sage for selected bg
val MintAccent      = Color(0xFF52A876)   // Accent highlight

// ─── Light Mode ──────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF4F7F5)   // Soft off-white background
val LightSurface    = Color(0xFFFFFFFF)   // Pure white card / surface
val LightSurfaceVar = Color(0xFFEDF2EE)   // Subtle tinted surface variant
val LightOutline    = Color(0xFFBDCCC2)   // Input / divider border
val LightText       = Color(0xFF1A1A1A)   // Primary text
val LightTextMuted  = Color(0xFF6B7B70)   // Secondary / caption text

// ─── Dark Mode ───────────────────────────────────────────────────────────────
val DarkBackground  = Color(0xFF121212)   // Rich charcoal background
val DarkSurface     = Color(0xFF1E1E1E)   // Slightly lighter card surface
val DarkSurfaceVar  = Color(0xFF2A2A2A)   // Elevated surface (nav, inputs)
val DarkOutline     = Color(0xFF3D3D3D)   // Subtle separator
val DarkText        = Color(0xFFF0F0F0)   // Primary text
val DarkTextMuted   = Color(0xFF9E9E9E)   // Secondary / caption text

// ─── Functional ──────────────────────────────────────────────────────────────
val ErrorRed        = Color(0xFFD32F2F)
val ErrorRedDark    = Color(0xFFEF5350)
val WarningAmber    = Color(0xFFF57C00)
val WarningAmberDark = Color(0xFFFFB74D)

// ─── Legacy aliases (used by existing screens) ───────────────────────────────
val PakistanGreen   = ForestGreen
val LuminousGreen   = MintAccent
val UrgentRed       = ErrorRed
val SoftRed         = ErrorRedDark
val DeepCharcoal    = DarkBackground
val SurfaceGrey     = DarkSurface
val CharcoalText    = LightText
val SlateText       = LightTextMuted
val White           = Color(0xFFFFFFFF)
val Black           = Color(0xFF000000)