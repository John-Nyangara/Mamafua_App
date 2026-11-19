package com.mamafua.app.presentation.worker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobProgress
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.ScopeAdjustment
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.presentation.client.StatusBadge
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class WorkerMyJobsViewModel(private val jobRepository: JobRepository) : ViewModel() {
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<Int, JobProgress>>(emptyMap())
    val progressMap: StateFlow<Map<Int, JobProgress>> = _progressMap.asStateFlow()

    private val _adjustmentsMap = MutableStateFlow<Map<Int, List<ScopeAdjustment>>>(emptyMap())
    val adjustmentsMap: StateFlow<Map<Int, List<ScopeAdjustment>>> = _adjustmentsMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val res = jobRepository.fetchMyAssignedJobs()) {
                is Resource.Success -> {
                    val list = res.data.orEmpty()
                    _jobs.value = list
                    list.forEach { job ->
                        refreshJobDetails(job.id)
                    }
                }
                is Resource.Error -> _error.value = res.message
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    fun refreshJobDetails(jobId: Int) {
        viewModelScope.launch {
            val progRes = jobRepository.getJobProgress(jobId)
            if (progRes is Resource.Success && progRes.data != null) {
                _progressMap.value = _progressMap.value + (jobId to progRes.data)
            }
            val adjRes = jobRepository.getScopeAdjustments(jobId)
            if (adjRes is Resource.Success && adjRes.data != null) {
                _adjustmentsMap.value = _adjustmentsMap.value + (jobId to adjRes.data)
            }
        }
    }

    fun markArrivedAtGate(jobId: Int) {
        viewModelScope.launch {
            when (val res = jobRepository.updateJobStatus(jobId, JobStatus.ARRIVED)) {
                is Resource.Success -> {
                    _actionMessage.value = "Marked Arrived at Gate! Inspect scope & enter the client's 4-digit Start PIN."
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Failed to mark arrived: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun verifyStartPin(jobId: Int, pin: String) {
        viewModelScope.launch {
            when (val res = jobRepository.verifyStartPin(jobId, pin.trim())) {
                is Resource.Success -> {
                    _actionMessage.value = "4-Digit Handshake PIN verified! Job timer started."
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Invalid Start PIN: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun requestScopeAdjustment(jobId: Int, additionalAmount: Double, reason: String) {
        viewModelScope.launch {
            when (val res = jobRepository.createScopeAdjustment(jobId, additionalAmount, reason)) {
                is Resource.Success -> {
                    _actionMessage.value = "Scope adjustment (+KSh ${additionalAmount.toInt()}) sent to client for approval!"
                    refreshJobDetails(jobId)
                }
                is Resource.Error -> {
                    _actionMessage.value = "Scope adjustment failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun markJobComplete(jobId: Int) {
        viewModelScope.launch {
            when (val res = jobRepository.markJobCompleteByWorker(jobId)) {
                is Resource.Success -> {
                    _actionMessage.value = "Completion requested! Client notified to verify & release M-Pesa escrow."
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Failed to mark complete: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}

@Composable
fun WorkerMyJobsScreen(
    viewModel: WorkerMyJobsViewModel = koinViewModel(),
    onOpenActiveJob: (Int) -> Unit = {}
) {
    val jobs by viewModel.jobs.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()
    val adjustmentsMap by viewModel.adjustmentsMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var jobForStartPin by remember { mutableStateOf<Job?>(null) }
    var jobForScopeAdjustment by remember { mutableStateOf<Job?>(null) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Job Checklist",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Arrived -> Scope Check -> 4-Digit PIN -> Complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.loadJobs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PremiumOrangeDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PremiumOrangeDark)
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadJobs() }) { Text("Retry") }
                }
            } else if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = PremiumOrangeDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No assigned jobs yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Switch ONLINE on the Radar tab to accept Eldoret jobs!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(jobs) { job ->
                        WorkerActiveJobCard(
                            job = job,
                            progress = progressMap[job.id],
                            adjustments = adjustmentsMap[job.id].orEmpty(),
                            onOpenDetail = { onOpenActiveJob(job.id) },
                            onMarkArrived = { viewModel.markArrivedAtGate(job.id) },
                            onOpenPinDialog = { jobForStartPin = job },
                            onOpenScopeDialog = { jobForScopeAdjustment = job },
                            onMarkComplete = { viewModel.markJobComplete(job.id) }
                        )
                    }
                }
            }
        }
    }

    jobForStartPin?.let { job ->
        StartPinDialog(
            jobTitle = job.title,
            onDismiss = { jobForStartPin = null },
            onVerify = { pin ->
                viewModel.verifyStartPin(job.id, pin)
                jobForStartPin = null
            }
        )
    }

    jobForScopeAdjustment?.let { job ->
        ScopeAdjustmentDialog(
            jobTitle = job.title,
            onDismiss = { jobForScopeAdjustment = null },
            onSubmit = { amount, reason ->
                viewModel.requestScopeAdjustment(job.id, amount, reason)
                jobForScopeAdjustment = null
            }
        )
    }
}

@Composable
fun WorkerActiveJobCard(
    job: Job,
    progress: JobProgress?,
    adjustments: List<ScopeAdjustment>,
    onOpenDetail: () -> Unit,
    onMarkArrived: () -> Unit,
    onOpenPinDialog: () -> Unit,
    onOpenScopeDialog: () -> Unit,
    onMarkComplete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PremiumOrangeDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.estate ?: job.location,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(status = job.status)
            }

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Booking Total: KSh ${job.budget.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumOrangeDark
                    )
                    Text(
                        text = "Your 90% Net Payout: KSh ${(job.budget * 0.9).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreenDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                progress?.client?.let { client ->
                    if (!client.phone.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call ${client.name.split(" ").firstOrNull() ?: "Client"}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val steps = listOf(
                Pair("1. Assigned", job.status != JobStatus.OPEN),
                Pair("2. Arrived", job.status == JobStatus.ARRIVED || job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.COMPLETION_REQUESTED || job.status == JobStatus.COMPLETED),
                Pair("3. PIN Started", job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.COMPLETION_REQUESTED || job.status == JobStatus.COMPLETED),
                Pair("4. Completed", job.status == JobStatus.COMPLETION_REQUESTED || job.status == JobStatus.COMPLETED)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEach { (label, done) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (done) SuccessGreenDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) SuccessGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (adjustments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                adjustments.forEach { adj ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scope +KSh ${adj.additionalAmount.toInt()}: ${adj.reason}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = adj.status.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (adj.status.lowercase()) {
                                    "approved" -> SuccessGreenDark
                                    "rejected" -> ErrorRedDark
                                    else -> WarningAmberDark
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            when (job.status) {
                JobStatus.ASSIGNED, JobStatus.ACCEPTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenScopeDialog,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Scope Add-on", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onMarkArrived,
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DoorFront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Arrived at Gate", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onOpenDetail) {
                            Text("Full Checklist")
                        }
                        TextButton(onClick = onOpenPinDialog) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enter 4-Digit Start PIN")
                        }
                    }
                }
                JobStatus.ARRIVED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenScopeDialog,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Scope (+KSh)", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onOpenPinDialog,
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enter 4-Digit Start PIN", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                JobStatus.IN_PROGRESS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenScopeDialog,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Extra Task", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onMarkComplete,
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Cleaning Complete", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                JobStatus.COMPLETION_REQUESTED -> {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Waiting for client confirmation to release M-Pesa escrow to your wallet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmberDark,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                JobStatus.COMPLETED -> {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Job Completed! Earnings credited to your Mama Fua Wallet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreenDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun StartPinDialog(
    jobTitle: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enter Client's 4-Digit Start PIN", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Ask the client at the gate/door for the 4-digit Start PIN shown on their app for \"$jobTitle\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it.filter { ch -> ch.isDigit() } },
                    label = { Text("4-Digit Start PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(pin) },
                enabled = pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark)
            ) {
                Text("Verify & Start Timer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ScopeAdjustmentDialog(
    jobTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("350") }
    var reason by remember { mutableStateOf("Extra heavy laundry basket / duvet found on-site") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Request Scope Adjustment", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "If there are more clothes, rooms, or heavy items than booked in \"$jobTitle\", request a transparent price adjustment before starting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Additional Amount (KSh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g. +1 Heavy Duvet, +Inside Fridge)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSubmit(parsedAmount, reason) },
                enabled = parsedAmount > 0 && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark)
            ) {
                Text("Send to Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
