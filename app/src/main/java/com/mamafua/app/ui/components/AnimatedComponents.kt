package com.mamafua.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowBack
import com.mamafua.app.ui.theme.*

// ══════════════════════════════════════════════════════════
// GRADIENT BUTTON — with press-scale animation
// ══════════════════════════════════════════════════════════

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = PrimaryGradientHorizontal,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 4.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = PremiumTealDark.copy(alpha = 0.3f),
                spotColor = PremiumTealDark.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = if (enabled) gradient else Brush.horizontalGradient(
                    listOf(Color.Gray, Color.Gray.copy(alpha = 0.7f))
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// GLASS CARD — semi-transparent with subtle border
// ══════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

// ══════════════════════════════════════════════════════════
// ANIMATED TEXT FIELD — with icon and focus glow
// ══════════════════════════════════════════════════════════

@Composable
fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PremiumTealDark,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = PremiumTealDark,
            cursorColor = PremiumTealDark,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ══════════════════════════════════════════════════════════
// SHIMMER LOADING — placeholder animation
// ══════════════════════════════════════════════════════════

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f
) {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ShimmerBaseLight,
                        ShimmerHighlightLight,
                        ShimmerBaseLight
                    ),
                    start = Offset(shimmerOffset, 0f),
                    end = Offset(shimmerOffset + 200f, 0f)
                )
            )
    )
}

// ══════════════════════════════════════════════════════════
// PULSING DOT — for availability status
// ══════════════════════════════════════════════════════════

@Composable
fun PulsingDot(
    color: Color = StatusAvailable,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(size * 1.8f)
                .scale(pulseScale)
                .graphicsLayer { alpha = pulseAlpha * 0.3f }
                .background(color = color, shape = CircleShape)
        )
        // Inner solid dot
        Box(
            modifier = Modifier
                .size(size)
                .background(color = color, shape = CircleShape)
        )
    }
}

// ══════════════════════════════════════════════════════════
// ANIMATED COUNTER — numbers animate when changed
// ══════════════════════════════════════════════════════════

@Composable
fun AnimatedCounter(
    targetValue: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "counter_alpha"
    )

    Text(
        text = targetValue,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier.graphicsLayer { alpha = animatedAlpha }
    )
}

// ══════════════════════════════════════════════════════════
// GRADIENT TOP BAR — header area with gradient background
// ══════════════════════════════════════════════════════════

@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    gradient: Brush = PrimaryGradientVertical,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Row(content = actions)
        }
    }
}

// ══════════════════════════════════════════════════════════
// STATUS CHIP — colored chip for status display
// ══════════════════════════════════════════════════════════

@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ══════════════════════════════════════════════════════════
// EMPTY STATE VIEW — friendly empty state with icon
// ══════════════════════════════════════════════════════════

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Info,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

// ══════════════════════════════════════════════════════════
// AVATAR CIRCLE — circular avatar with gradient border
// ══════════════════════════════════════════════════════════

@Composable
fun AvatarCircle(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    borderGradient: Brush = PrimaryGradientHorizontal,
    backgroundColor: Color = PremiumTealDark
) {
    val initials = name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size + 4.dp)
            .background(brush = borderGradient, shape = CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
// PREMIUM ELEVATED CARD — with subtle shadow and rounded corners
// ══════════════════════════════════════════════════════════

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

// ══════════════════════════════════════════════════════════
// INFO ROW — icon + label + value row for profile screens
// ══════════════════════════════════════════════════════════

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
// STAT CARD — small stat display for dashboard
// ══════════════════════════════════════════════════════════

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
