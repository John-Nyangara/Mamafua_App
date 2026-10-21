package com.mamafua.app.presentation.auth.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mamafua.app.ui.components.AnimatedTextField
import com.mamafua.app.ui.components.GradientButton
import com.mamafua.app.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
        contentOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(brush = PrimaryGradientVertical)
        ) {
            // Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(top = 44.dp, start = 8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            alpha = contentAlpha.value
                            scaleX = contentAlpha.value
                            scaleY = contentAlpha.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                )
            }
        }

        // ── White Card Body ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 260.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentOffset.value
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AnimatedTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "Send Reset Link",
                onClick = { /* TODO: Implement Reset Logic */ },
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateBack) {
                Text(
                    "Back to Login",
                    color = PremiumTealDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
            }
        }
    }
}
