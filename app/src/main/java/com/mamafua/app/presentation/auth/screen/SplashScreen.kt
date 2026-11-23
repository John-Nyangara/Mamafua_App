package com.mamafua.app.presentation.auth.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mamafua.app.presentation.main.viewmodel.AuthState
import com.mamafua.app.ui.theme.PremiumTeal
import com.mamafua.app.ui.theme.SplashGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authState: AuthState,
    onSplashComplete: (String) -> Unit
) {
    var isTimerDone by remember { mutableStateOf(false) }

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(30f) }
    val subtitleAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        logoScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(150)
        textAlpha.animateTo(1f, animationSpec = tween(400))
        textOffset.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        delay(150)
        subtitleAlpha.animateTo(1f, animationSpec = tween(350))
        delay(450)
        isTimerDone = true
    }

    LaunchedEffect(isTimerDone, authState) {
        if (isTimerDone && authState !is AuthState.Loading) {
            val targetRoute = when (authState) {
                is AuthState.AuthenticatedClient -> "client_main"
                is AuthState.AuthenticatedWorker -> "worker_main"
                is AuthState.AuthenticatedAdmin -> "admin_main"
                is AuthState.Unauthenticated -> "login"
                is AuthState.Loading -> "login"
            }
            onSplashComplete(targetRoute)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = SplashGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(color = PremiumTeal.copy(alpha = 0.3f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale * 0.85f)
                        .graphicsLayer { alpha = pulseAlpha * 0.6f }
                        .background(color = PremiumTeal.copy(alpha = 0.2f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .background(color = Color.White.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MF",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Mama Fua",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffset.value
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Eldoret's Trusted Laundry & Home Cleaning",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )
        }
    }
}
