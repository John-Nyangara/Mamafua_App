package com.mamafua.app.presentation.worker

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class WorkerProfileViewModel(
    userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val user = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogout()
        }
    }
}

@Composable
fun WorkerProfileScreen(
    viewModel: WorkerProfileViewModel = koinViewModel(),
    onLoggedOut: () -> Unit
) {
    val user by viewModel.user.collectAsState()

    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient Header with Avatar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(PremiumOrangeDark, PremiumOrange)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarCircle(
                    name = user?.name ?: "Worker",
                    size = 80.dp,
                    borderGradient = AccentGradient,
                    backgroundColor = PremiumOrangeDark,
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha.value
                        scaleX = contentAlpha.value
                        scaleY = contentAlpha.value
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                )
            }
        }

        // ── Profile Info Cards ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer { alpha = contentAlpha.value },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
            if (user != null) {
                PremiumCard {
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = user?.name ?: "—"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    InfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user?.email ?: "—"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = user?.phone ?: "—"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    InfoRow(
                        icon = Icons.Default.Badge,
                        label = "Role",
                        value = user?.role ?: "—",
                        iconTint = PremiumOrangeDark
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    InfoRow(
                        icon = if (user?.isVerified == true) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        label = "Verification",
                        value = if (user?.isVerified == true) "Verified ✓" else "Not Verified",
                        iconTint = if (user?.isVerified == true) SuccessGreenDark else WarningAmberDark
                    )
                }
            } else {
                PremiumCard {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Logout Button ──
            GradientButton(
                text = "Logout",
                onClick = { viewModel.logout(onLoggedOut) },
                gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(ErrorRed, ErrorRedDark)
                ),
                icon = Icons.Default.Logout
            )

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
