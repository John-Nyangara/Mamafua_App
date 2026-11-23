package com.mamafua.app.presentation.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.PayoutEntry
import com.mamafua.app.domain.model.WorkerEarningsData
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class EarningsViewModel(
    private val workerRepository: WorkerRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _earnings = MutableStateFlow(WorkerEarningsData())
    val earnings: StateFlow<WorkerEarningsData> = _earnings.asStateFlow()

    private val _defaultPhone = MutableStateFlow("2547")
    val defaultPhone: StateFlow<String> = _defaultPhone.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isWithdrawing = MutableStateFlow(false)
    val isWithdrawing: StateFlow<Boolean> = _isWithdrawing.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadEarnings()
        loadUserPhone()
    }

    private fun loadUserPhone() {
        viewModelScope.launch {
            runCatching { userRepository.getProfile() }.onSuccess { user ->
                if (user.phone.isNotBlank()) {
                    _defaultPhone.value = user.phone
                }
            }
        }
    }

    fun loadEarnings() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val res = workerRepository.getEarnings()) {
                is Resource.Success -> {
                    if (res.data != null) {
                        _earnings.value = res.data
                    }
                }
                is Resource.Error -> {
                    _actionMessage.value = "Could not refresh wallet: ${res.message}"
                }
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    fun withdrawToMpesa(phoneNumber: String) {
        viewModelScope.launch {
            _isWithdrawing.value = true
            when (val res = workerRepository.withdrawEarnings(phoneNumber.trim())) {
                is Resource.Success -> {
                    _actionMessage.value = res.data ?: "Instant M-Pesa withdrawal initiated!"
                    loadEarnings()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Withdrawal failed: ${res.message}"
                }
                else -> Unit
            }
            _isWithdrawing.value = false
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}

@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel = koinViewModel()
) {
    val earnings by viewModel.earnings.collectAsState()
    val defaultPhone by viewModel.defaultPhone.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWithdrawing by viewModel.isWithdrawing.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Earnings & M-Pesa Wallet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Instant B2C M-Pesa Payouts • 90% Worker Share",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.loadEarnings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PremiumOrangeDark)
                    }
                }
            }

            // Hero Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PremiumOrangeDark, PremiumOrange)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Available to Withdraw",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KSh ${earnings.summary.pendingPayoutBalance.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Today's Net", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("KSh ${earnings.summary.todayNetEarnings.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Last 7 Days", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("KSh ${earnings.summary.weeklyNetEarnings.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Total Paid Out", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("KSh ${earnings.summary.paidOutBalance.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showWithdrawDialog = true },
                            enabled = !isWithdrawing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PremiumOrangeDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWithdrawing) "Disbursing to M-Pesa..." else "Instant Withdraw to M-Pesa",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Summary Breakdown Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Gross Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "KSh ${earnings.summary.totalGrossEarnings.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Lifetime Net (90%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "KSh ${earnings.summary.totalNetEarnings.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenDark
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Escrow Releases & M-Pesa Payouts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PremiumOrangeDark)
                    }
                }
            } else if (earnings.payouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PremiumOrangeDark.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No completed job payouts yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Once a client confirms job completion, your 90% payout appears here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(earnings.payouts) { entry ->
                    PayoutLedgerRow(entry)
                }
            }
        }
    }

    if (showWithdrawDialog) {
        var phone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Instant M-Pesa Withdrawal", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Disburse KSh ${earnings.summary.pendingPayoutBalance.toInt()} directly to your M-Pesa phone number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                        viewModel.withdrawToMpesa(phone)
                        showWithdrawDialog = false
                    },
                    enabled = phone.length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark)
                ) {
                    Text("Withdraw Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PayoutLedgerRow(entry: PayoutEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.jobTitle ?: "Job #${entry.jobId ?: entry.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gross KSh ${entry.grossAmount.toInt()} • Fee KSh ${entry.platformFee.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.mpesaReceipt.isNullOrBlank()) {
                    Text(
                        text = "M-Pesa Ref: ${entry.mpesaReceipt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumTealDark
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+KSh ${entry.netAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SuccessGreenDark
                )
                Text(
                    text = entry.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.status.equals("paid", true) || entry.status.equals("completed", true)) SuccessGreenDark else WarningAmberDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
