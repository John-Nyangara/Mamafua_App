package com.mamafua.app.presentation.client

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.data.remote.UpdateUserRequest
import com.mamafua.app.domain.model.BlockedUser
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class ClientProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val user = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers = _blockedUsers.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback = _feedback.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            val res = userRepository.getBlockedUsers()
            if (res is Resource.Success) {
                _blockedUsers.value = res.data ?: emptyList()
            }
        }
    }

    fun updateProfile(name: String, email: String, phone: String) {
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = userRepository.updateProfile(
                UpdateUserRequest(name = name.trim(), email = email.trim(), phone = phone.trim())
            )) {
                is Resource.Success -> _feedback.value = "Profile updated successfully!"
                is Resource.Error -> _feedback.value = res.message ?: "Failed to update profile"
                else -> Unit
            }
            _isBusy.value = false
        }
    }

    fun switchToWorkerRole(onSwitched: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = userRepository.switchRole("worker")) {
                is Resource.Success -> {
                    _isBusy.value = false
                    onSwitched()
                }
                is Resource.Error -> {
                    _isBusy.value = false
                    _feedback.value = res.message ?: "Failed to switch role"
                }
                else -> _isBusy.value = false
            }
        }
    }

    fun unblockUser(userId: Int) {
        viewModelScope.launch {
            when (val res = userRepository.unblockUser(userId)) {
                is Resource.Success -> {
                    _feedback.value = "User unblocked"
                    loadBlockedUsers()
                }
                is Resource.Error -> _feedback.value = res.message
                else -> Unit
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }
}

@Composable
fun ClientProfileScreen(
    viewModel: ClientProfileViewModel = koinViewModel(),
    onRoleSwitched: () -> Unit = {},
    onLoggedOut: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val context = LocalContext.current

    var showEditModal by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        feedback?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = PrimaryGradientVertical)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarCircle(name = user?.name ?: "Client", size = 80.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (user != null) {
                    PremiumCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showEditModal = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                        InfoRow(icon = Icons.Default.Person, label = "Full Name", value = user?.name ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        InfoRow(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        InfoRow(icon = Icons.Default.Phone, label = "M-Pesa Phone", value = user?.phone ?: "—")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        InfoRow(
                            icon = Icons.Default.Badge,
                            label = "Active Mode",
                            value = (user?.role ?: "client").uppercase(),
                            iconTint = PremiumTealDark
                        )
                    }
                }

                // Switch Role Card
                PremiumCard {
                    Text(
                        text = "Earn as a Mama Fua",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch to Mama Fua (Worker) mode to receive Job Radar dispatches and instant M-Pesa payouts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Switch to Mama Fua (Worker) Mode",
                        onClick = { viewModel.switchToWorkerRole(onRoleSwitched) },
                        gradient = AccentGradient,
                        isLoading = isBusy,
                        icon = Icons.Default.SwapHoriz
                    )
                }

                // Blocked Users Section
                if (blockedUsers.isNotEmpty()) {
                    PremiumCard {
                        Text(
                            text = "Blocked Users (${blockedUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        blockedUsers.forEach { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b.name, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    viewModel.unblockUser(if (b.blockedUserId != 0) b.blockedUserId else b.id)
                                }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }

                GradientButton(
                    text = "Logout",
                    onClick = { viewModel.logout(onLoggedOut) },
                    gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(ErrorRed, ErrorRedDark)),
                    icon = Icons.Default.Logout
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEditModal && user != null) {
        var name by remember { mutableStateOf(user?.name ?: "") }
        var email by remember { mutableStateOf(user?.email ?: "") }
        var phone by remember { mutableStateOf(user?.phone ?: "") }

        AlertDialog(
            onDismissRequest = { showEditModal = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("M-Pesa Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(name, email, phone)
                        showEditModal = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
