package com.mamafua.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Primary Teal Palette ──
val PremiumTeal = Color(0xFF00B4D8)
val PremiumTealDark = Color(0xFF0077B6)
val PremiumTealLight = Color(0xFF90E0EF)
val PremiumTealSurface = Color(0xFFCAF0F8)

// ── Deep Blue Palette ──
val PremiumBlue = Color(0xFF03045E)
val PremiumBlueMid = Color(0xFF023E8A)
val PremiumBlueLight = Color(0xFF0096C7)

// ── Orange Accent Palette ──
val PremiumOrange = Color(0xFFFF9F1C)
val PremiumOrangeDark = Color(0xFFF77F00)
val PremiumOrangeLight = Color(0xFFFFBF69)
val PremiumOrangeSurface = Color(0xFFFFF3E0)

// ── Semantic Colors ──
val SuccessGreen = Color(0xFF2DC653)
val SuccessGreenDark = Color(0xFF1B9E40)
val SuccessGreenSurface = Color(0xFFE8F5E9)
val ErrorRed = Color(0xFFEF476F)
val ErrorRedDark = Color(0xFFD32F2F)
val ErrorRedSurface = Color(0xFFFFEBEE)
val WarningAmber = Color(0xFFFFC107)
val WarningAmberDark = Color(0xFFFF8F00)

// ── Compatibility Aliases ──
val PremiumPurple = PremiumTeal
val PremiumPurpleDark = PremiumTealDark
val PremiumPurpleSurface = PremiumTealSurface
val WarningYellow = WarningAmber

// ── Status Colors ──
val StatusAvailable = Color(0xFF2DC653)
val StatusBusy = Color(0xFFFFC107)
val StatusOffline = Color(0xFF9E9E9E)

// ── M-Pesa Green ──
val MpesaGreen = Color(0xFF4CAF50)
val MpesaGreenDark = Color(0xFF388E3C)

// ── Background & Surface ──
val PremiumBackgroundLight = Color(0xFFF0F4F8)
val PremiumBackgroundDark = Color(0xFF0D1117)
val PremiumCardLight = Color(0xFFFFFFFF)
val PremiumCardDark = Color(0xFF161B22)
val PremiumSurfaceElevated = Color(0xFF1C2128)

// ── Text Colors ──
val TextPrimaryLight = Color(0xFF1A1A2E)
val TextPrimaryDark = Color(0xFFF0F6FC)
val TextSecondaryLight = Color(0xFF6B7280)
val TextSecondaryDark = Color(0xFF8B949E)

// ── Shimmer Colors ──
val ShimmerBaseLight = Color(0xFFE0E0E0)
val ShimmerHighlightLight = Color(0xFFF5F5F5)
val ShimmerBaseDark = Color(0xFF2D2D2D)
val ShimmerHighlightDark = Color(0xFF3D3D3D)

// ── Glass Morphism ──
val GlassOverlayLight = Color(0x40FFFFFF)
val GlassOverlayDark = Color(0x30FFFFFF)
val GlassBorderLight = Color(0x50FFFFFF)
val GlassBorderDark = Color(0x20FFFFFF)

// ── Gradient Brushes ──
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(PremiumTeal, PremiumTealDark, PremiumBlueMid),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val PrimaryGradientVertical = Brush.verticalGradient(
    colors = listOf(PremiumTeal, PremiumTealDark, PremiumBlue)
)

val PrimaryGradientHorizontal = Brush.horizontalGradient(
    colors = listOf(PremiumTeal, PremiumTealDark)
)

val AccentGradient = Brush.horizontalGradient(
    colors = listOf(PremiumOrange, PremiumOrangeDark)
)

val SplashGradient = Brush.verticalGradient(
    colors = listOf(PremiumTealDark, PremiumBlueMid, PremiumBlue)
)

val CardGradientLight = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF8FBFF))
)

val CardGradientDark = Brush.verticalGradient(
    colors = listOf(PremiumCardDark, Color(0xFF0D1117))
)

fun headerGradient(isDark: Boolean = false) = Brush.verticalGradient(
    colors = if (isDark) {
        listOf(PremiumBlueMid, PremiumBlue)
    } else {
        listOf(PremiumTeal, PremiumTealDark)
    }
)

fun mpesaGradient() = Brush.horizontalGradient(
    colors = listOf(MpesaGreen, MpesaGreenDark)
)
