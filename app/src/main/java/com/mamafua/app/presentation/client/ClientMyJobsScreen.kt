package com.mamafua.app.presentation.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.mamafua.app.domain.repository.UserRepository

import com.mamafua.app.domain.repository.PaymentRepository

class ClientMyJobsViewModel(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    val jobs = jobRepository.getActiveJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        fetchJobs()
    }

    private fun fetchJobs() {
        viewModelScope.launch {
            try {
                jobRepository.fetchMyPostedJobs()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to fetch jobs"
            }
        }
    }

    fun acceptApplication(jobId: Int, workerId: Int = 2) {
        viewModelScope.launch {
            try {
                jobRepository.acceptWorker(jobId, workerId)
                fetchJobs()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to accept application"
            }
        }
    }

    fun cancelJob(jobId: Int) {
        viewModelScope.launch {
            try {
                jobRepository.cancelJob(jobId)
                fetchJobs()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to cancel job"
            }
        }
    }

    fun confirmCompletion(jobId: Int) {
        viewModelScope.launch {
            try {
                jobRepository.confirmCompletion(jobId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to confirm completion"
            }
        }
    }

    fun updateJobStatus(jobId: Int, status: com.mamafua.app.domain.model.JobStatus) {
        viewModelScope.launch {
            try {
                jobRepository.updateJobStatus(jobId, status)
                fetchJobs()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update job status"
            }
        }
    }

    fun createReview(jobId: Int, rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                jobRepository.createReview(jobId, rating, comment)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to submit review"
            }
        }
    }

    fun raiseDispute(jobId: Int, reason: String, description: String) {
        viewModelScope.launch {
            try {
                jobRepository.raiseDispute(jobId, reason, description)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to raise dispute"
            }
        }
    }

    fun initiateSTKPush(jobId: Int, phone: String) {
        viewModelScope.launch {
            try {
                paymentRepository.initiateSTKPush(jobId, phone)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to initiate M-Pesa payment"
            }
        }
    }
}

@Composable
fun ClientMyJobsScreen(
    viewModel: ClientMyJobsViewModel = koinViewModel(),
    onNavigateToApplicants: (Int) -> Unit
) {
    val jobs by viewModel.jobs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    var jobToReview by remember { mutableStateOf<Int?>(null) }
    var jobToDispute by remember { mutableStateOf<Int?>(null) }
    var jobToPay by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "My Jobs",
            subtitle = "${jobs.size} total jobs"
        )

        // ── Content ──
        if (jobs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.WorkOff,
                title = "No Jobs Posted Yet",
                subtitle = "Post your first job and find trusted workers",
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 800.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(jobs) { index, job ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 80L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        )
                    ) {
                        PremiumCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusChip(
                                    text = job.status.name,
                                    color = when (job.status) {
                                        com.mamafua.app.domain.model.JobStatus.OPEN -> PremiumTealDark
                                        com.mamafua.app.domain.model.JobStatus.IN_PROGRESS -> WarningAmberDark
                                        com.mamafua.app.domain.model.JobStatus.COMPLETED -> SuccessGreenDark
                                        com.mamafua.app.domain.model.JobStatus.CANCELLED -> ErrorRedDark
                                        com.mamafua.app.domain.model.JobStatus.DISPUTED -> ErrorRedDark
                                        else -> StatusOffline
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = PremiumOrangeDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KES ${job.budget}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PremiumOrangeDark
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = job.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (job.status == com.mamafua.app.domain.model.JobStatus.OPEN || job.status == com.mamafua.app.domain.model.JobStatus.APPLIED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GradientButton(
                                        text = "View Applicants",
                                        onClick = { onNavigateToApplicants(job.id) },
                                        gradient = AccentGradient,
                                        icon = Icons.Default.Group,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelJob(job.id) },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text("Cancel", color = ErrorRedDark)
                                    }
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.ASSIGNED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { jobToDispute = job.id },
                                        modifier = Modifier.height(56.dp).fillMaxWidth()
                                    ) {
                                        Text("Dispute", color = ErrorRedDark)
                                    }
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.IN_PROGRESS) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GradientButton(
                                        text = "Mark Completed",
                                        onClick = { viewModel.confirmCompletion(job.id) },
                                        gradient = PrimaryGradientHorizontal,
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { jobToDispute = job.id },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text("Dispute", color = ErrorRedDark)
                                    }
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.COMPLETED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                if (job.paymentStatus?.equals("completed", ignoreCase = true) != true) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GradientButton(
                                            text = "Pay",
                                            onClick = { jobToPay = job.id },
                                            gradient = mpesaGradient(),
                                            icon = Icons.Default.AccountBalanceWallet,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    GradientButton(
                                        text = "Leave Review",
                                        onClick = { jobToReview = job.id },
                                        gradient = AccentGradient,
                                        icon = Icons.Default.Star,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.DISPUTED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Job is under dispute. Support will contact you soon.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ErrorRedDark,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    jobToReview?.let { jobId ->
        ReviewDialog(
            onDismiss = { jobToReview = null },
            onSubmit = { rating, comment ->
                viewModel.createReview(jobId, rating, comment)
                jobToReview = null
            }
        )
    }

    jobToDispute?.let { jobId ->
        DisputeDialog(
            onDismiss = { jobToDispute = null },
            onSubmit = { reason, description ->
                viewModel.raiseDispute(jobId, reason, description)
                jobToDispute = null
            }
        )
    }

    jobToPay?.let { jobId ->
        PaymentDialog(
            defaultPhone = currentUser?.phone ?: "",
            onDismiss = { jobToPay = null },
            onSubmit = { phone ->
                viewModel.initiateSTKPush(jobId, phone)
                jobToPay = null
            }
        )
    }
}

@Composable
fun PaymentDialog(
    defaultPhone: String,
    onDismiss: () -> Unit,
    onSubmit: (phone: String) -> Unit
) {
    var phone by remember { mutableStateOf(defaultPhone) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pay with M-Pesa", fontWeight = FontWeight.Bold, color = MpesaGreenDark)
        },
        text = {
            Column {
                Text(
                    "Enter your M-Pesa phone number to initiate an STK Push for this job.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. 0712345678)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (phone.isNotBlank()) {
                        onSubmit(phone)
                    }
                }
            ) {
                Text("Send Prompt", color = MpesaGreenDark, fontWeight = FontWeight.Bold)
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
fun ReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("How was the service?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, comment) }) {
                Text("Submit")
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
fun DisputeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raise Dispute", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("What went wrong?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g., No show, Poor quality)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason, description) },
                enabled = reason.isNotBlank() && description.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )}
