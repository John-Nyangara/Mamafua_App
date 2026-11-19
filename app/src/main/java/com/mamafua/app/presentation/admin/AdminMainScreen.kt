package com.mamafua.app.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Dispute
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.PaymentRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class AdminPortalUiState(
    val isLoading: Boolean = false,
    val disputes: List<Dispute> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class AdminPortalViewModel(
    private val jobRepository: JobRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPortalUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDisputes()
    }

    fun loadDisputes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = jobRepository.getAllDisputes()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        disputes = res.data ?: emptyList()
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> Unit
            }
        }
    }

    fun resolveDispute(disputeId: Int, status: String, notes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = jobRepository.resolveDispute(disputeId, status, notes)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Dispute #$disputeId marked as $status"
                    )
                    loadDisputes()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> Unit
            }
        }
    }

    fun releaseJobEscrow(jobId: Int) {
        viewModelScope.launch {
            when (val res = paymentRepository.releaseJobEscrow(jobId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(message = "Escrow released for Job #$jobId")
                is Resource.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                else -> Unit
            }
        }
    }

    fun triggerAutoRelease() {
        viewModelScope.launch {
            when (val res = paymentRepository.triggerAutoReleaseEscrow()) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(message = "Auto-release completed for eligible jobs!")
                is Resource.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                else -> Unit
            }
        }
    }

    fun switchRole(role: String, onSwitched: () -> Unit) {
        viewModelScope.launch {
            when (val res = userRepository.switchRole(role)) {
                is Resource.Success -> onSwitched()
                is Resource.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                else -> Unit
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

@Composable
fun AdminMainScreen(
    viewModel: AdminPortalViewModel = koinViewModel(),
    onRoleSwitched: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("all") }
    var disputeToResolve by remember { mutableStateOf<Dispute?>(null) }
    var manualJobId by remember { mutableStateOf("") }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    val filteredDisputes = remember(uiState.disputes, selectedFilter) {
        if (selectedFilter == "all") uiState.disputes
        else uiState.disputes.filter { it.status.equals(selectedFilter, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Admin Portal",
            subtitle = "Dispute Resolution & Escrow Controls",
            gradient = Brush.verticalGradient(listOf(PremiumBlue, PremiumBlueMid)),
            actions = {
                IconButton(onClick = { viewModel.loadDisputes() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
                IconButton(onClick = { viewModel.logout(onLogout) }) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Escrow Management Card
            item {
                PremiumCard {
                    Text(
                        text = "M-Pesa Escrow Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Auto-release uncontested completion_requested jobs or manually release escrow for a specific job.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Run Timeout Auto-Release",
                        onClick = { viewModel.triggerAutoRelease() },
                        gradient = mpesaGradient(),
                        icon = Icons.Default.AutoMode
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualJobId,
                            onValueChange = { manualJobId = it },
                            label = { Text("Job ID") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                manualJobId.toIntOrNull()?.let { id ->
                                    viewModel.releaseJobEscrow(id)
                                    manualJobId = ""
                                }
                            },
                            enabled = manualJobId.toIntOrNull() != null,
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Release B2C")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.switchRole("client", onRoleSwitched) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Client View")
                        }
                        OutlinedButton(
                            onClick = { viewModel.switchRole("worker", onRoleSwitched) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Worker View")
                        }
                    }
                }
            }

            // Dispute Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("all" to "All", "open" to "Open", "resolved" to "Resolved", "dismissed" to "Dismissed")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label) }
                            )
                        }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredDisputes.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Gavel,
                        title = "No Disputes Found",
                        subtitle = "All platform bookings are running smoothly."
                    )
                }
            } else {
                items(filteredDisputes, key = { it.id }) { dispute ->
                    PremiumCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dispute #${dispute.id} • Job #${dispute.jobId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusChip(
                                text = dispute.status.uppercase(),
                                color = when (dispute.status.lowercase()) {
                                    "open", "pending" -> ErrorRedDark
                                    "resolved" -> SuccessGreenDark
                                    else -> StatusOffline
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reason: ${dispute.reason}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dispute.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        dispute.resolutionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Resolution: $notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreenDark
                            )
                        }
                        if (dispute.status.equals("open", ignoreCase = true) ||
                            dispute.status.equals("pending", ignoreCase = true)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            GradientButton(
                                text = "Resolve / Dismiss Dispute",
                                onClick = { disputeToResolve = dispute },
                                icon = Icons.Default.Gavel
                            )
                        }
                    }
                }
            }
        }
    }

    disputeToResolve?.let { dispute ->
        var resolutionNotes by remember { mutableStateOf("") }
        var actionStatus by remember { mutableStateOf("resolved") }

        AlertDialog(
            onDismissRequest = { disputeToResolve = null },
            title = { Text("Resolve Dispute #${dispute.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = actionStatus == "resolved",
                            onClick = { actionStatus = "resolved" },
                            label = { Text("Resolve") }
                        )
                        FilterChip(
                            selected = actionStatus == "dismissed",
                            onClick = { actionStatus = "dismissed" },
                            label = { Text("Dismiss") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text("Admin Resolution Notes") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resolveDispute(dispute.id, actionStatus, resolutionNotes)
                        disputeToResolve = null
                    }
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { disputeToResolve = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
