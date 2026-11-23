package com.mamafua.app.presentation.auth.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mamafua.app.presentation.auth.viewmodel.RegisterEvent
import com.mamafua.app.presentation.auth.viewmodel.RegisterViewModel
import com.mamafua.app.ui.components.AnimatedTextField
import com.mamafua.app.ui.components.GradientButton
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value

    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
        contentOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onRegisterSuccess()
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(brush = PrimaryGradientVertical)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 70.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Join\nMama Fua",
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
                    text = "Create your account to get started",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                )
            }
        }

        // ── White Card Body ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 210.dp)
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
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Full Name
            AnimatedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(RegisterEvent.NameChanged(it)) },
                label = "Full Name",
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            AnimatedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone
            AnimatedTextField(
                value = state.phone,
                onValueChange = { viewModel.onEvent(RegisterEvent.PhoneChanged(it)) },
                label = "Phone Number",
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            AnimatedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Role Selection — Premium Card Selector
            Text(
                text = "I want to:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    title = "Find Help",
                    subtitle = "I need workers",
                    icon = Icons.Default.Home,
                    isSelected = state.role == "client",
                    onClick = { viewModel.onEvent(RegisterEvent.RoleChanged("client")) },
                    modifier = Modifier.weight(1f)
                )
                RoleCard(
                    title = "Find Work",
                    subtitle = "I'm a worker",
                    icon = Icons.Default.Build,
                    isSelected = state.role == "worker",
                    onClick = { viewModel.onEvent(RegisterEvent.RoleChanged("worker")) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error
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

            // Register Button
            GradientButton(
                text = "Create Account",
                onClick = { viewModel.onEvent(RegisterEvent.Submit) },
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                icon = Icons.Default.PersonAdd
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PremiumTealDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PremiumTealDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "role_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PremiumTealSurface else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "role_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) PremiumTealDark else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "role_icon"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "role_scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PremiumTealDark else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
