package com.mamafua.app.presentation.auth.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.components.AnimatedTextField
import com.mamafua.app.ui.components.GradientButton
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class ForgotPasswordUiState(
    val step: Int = 1, // 1 = request token, 2 = enter token & new password
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val resetCompleted: Boolean = false
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun requestResetToken(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            when (val res = authRepository.forgotPassword(email)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        step = 2,
                        message = res.data ?: "Reset token sent! Enter the token and your new password below."
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = res.message ?: "Failed to request reset token"
                    )
                }
                else -> Unit
            }
        }
    }

    fun submitNewPassword(token: String, newPassword: String) {
        if (token.isBlank() || newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(
                error = "Enter a valid reset token and a password of at least 6 characters"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            when (val res = authRepository.resetPassword(token, newPassword)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        resetCompleted = true,
                        message = res.data ?: "Password reset successfully!"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = res.message ?: "Failed to reset password"
                    )
                }
                else -> Unit
            }
        }
    }

    fun switchToStep(step: Int) {
        _uiState.value = _uiState.value.copy(step = step, error = null)
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
        contentOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(brush = PrimaryGradientVertical)
        ) {
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
                    .padding(top = 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(color = Color.White.copy(alpha = 0.15f), shape = CircleShape)
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
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (uiState.step == 1) "Forgot Password" else "Set New Password",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                )
            }
        }

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
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.message?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SuccessGreenSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreenDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = msg, color = SuccessGreenDark, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                uiState.error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRedSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = err,
                            color = ErrorRedDark,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (uiState.resetCompleted) {
                    GradientButton(
                        text = "Return to Sign In",
                        onClick = onNavigateBack,
                        icon = Icons.Default.CheckCircle
                    )
                } else if (uiState.step == 1) {
                    Text(
                        text = "Enter your registered email address to receive a password reset token.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    AnimatedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GradientButton(
                        text = "Request Reset Token",
                        onClick = { viewModel.requestResetToken(email) },
                        isLoading = uiState.isLoading,
                        enabled = !uiState.isLoading,
                        icon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { viewModel.switchToStep(2) }) {
                        Text("I already have a reset token", color = PremiumTealDark)
                    }
                } else {
                    Text(
                        text = "Enter the reset token you received and choose a new password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    AnimatedTextField(
                        value = resetToken,
                        onValueChange = { resetToken = it },
                        label = "Reset Token",
                        leadingIcon = Icons.Default.Key
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New Password",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GradientButton(
                        text = "Reset Password",
                        onClick = { viewModel.submitNewPassword(resetToken, newPassword) },
                        isLoading = uiState.isLoading,
                        enabled = !uiState.isLoading,
                        icon = Icons.Default.Lock
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { viewModel.switchToStep(1) }) {
                        Text("Request a new token instead", color = PremiumTealDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
