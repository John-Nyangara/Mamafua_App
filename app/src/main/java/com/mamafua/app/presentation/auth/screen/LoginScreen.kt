package com.mamafua.app.presentation.auth.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mamafua.app.presentation.auth.viewmodel.LoginEvent
import com.mamafua.app.presentation.auth.viewmodel.LoginViewModel
import com.mamafua.app.ui.components.AnimatedTextField
import com.mamafua.app.ui.components.GradientButton
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value

    // Entrance animation
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
        contentOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(brush = PrimaryGradientVertical)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 80.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Welcome\nBack ",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value * -1
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to continue to Mama Fua",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha.value
                    }
                )
            }
        }

        // ── White Card Body ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 240.dp)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Email field
            AnimatedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            AnimatedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Forgot password
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { /* TODO: Navigate to forgot password */ }) {
                    Text(
                        "Forgot Password?",
                        color = PremiumTealDark,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.error,
                        color = ErrorRedDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Login button
            GradientButton(
                text = "Login",
                onClick = { viewModel.onEvent(LoginEvent.Submit) },
                enabled = !state.isLoading,
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    text = "  or  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onNavigateToRegister,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Register",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PremiumTealDark
                    )
                }
            }
        }
    }
}
}

