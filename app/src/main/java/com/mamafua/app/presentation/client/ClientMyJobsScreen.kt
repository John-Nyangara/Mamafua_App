package com.mamafua.app.presentation.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobProgress
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.ScopeAdjustment
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.PaymentRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class ClientMyJobsViewModel(
    private val jobRepository: JobRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) : ViewModel() {
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
            when (val res = jobRepository.fetchMyPostedJobs()) {
                is Resource.Success -> {
                    val list = res.data.orEmpty()
                    _jobs.value = list
                    list.forEach { job ->
                        if (job.status != JobStatus.OPEN && job.status != JobStatus.CANCELLED) {
                            refreshJobDetails(job.id)
                        }
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

    fun autoDispatchJob(jobId: Int) {
        viewModelScope.launch {
            _actionMessage.value = "Broadcasting job to nearby online Mama Fuas..."
            when (val res = jobRepository.autoDispatchJob(jobId)) {
                is Resource.Success -> {
                    _actionMessage.value = "Auto-dispatch ping sent to nearby Mama Fuas!"
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Auto-dispatch failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun respondToScopeAdjustment(jobId: Int, adjustmentId: Int, action: String) {
        viewModelScope.launch {
            when (val res = jobRepository.respondToScopeAdjustment(jobId, adjustmentId, action)) {
                is Resource.Success -> {
                    _actionMessage.value = if (action == "approve") {
                        "Scope adjustment approved! Budget updated."
                    } else {
                        "Scope adjustment rejected."
                    }
                    loadJobs()
                    refreshJobDetails(jobId)
                }
                is Resource.Error -> {
                    _actionMessage.value = "Action failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun deleteJob(jobId: Int) {
        viewModelScope.launch {
            when (val res = jobRepository.cancelJob(jobId)) {
                is Resource.Success -> {
                    _actionMessage.value = "Booking cancelled"
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Cancel failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun verifyAndReviewJob(
        jobId: Int,
        workerUserId: Int?,
        rating: Int,
        comment: String,
        addToFavorites: Boolean
    ) {
        viewModelScope.launch {
            when (val res = jobRepository.confirmCompletion(jobId)) {
                is Resource.Success -> {
                    jobRepository.createReview(jobId, rating, comment.takeIf { it.isNotBlank() })
                    if (addToFavorites && workerUserId != null && workerUserId > 0) {
                        userRepository.addFavorite(workerUserId, comment.takeIf { it.isNotBlank() })
                    }
                    _actionMessage.value = "Completion verified! M-Pesa escrow released to Mama Fua."
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Verification failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun initiatePayment(jobId: Int, phoneNumber: String, paymentType: String) {
        viewModelScope.launch {
            _actionMessage.value = "Sending M-Pesa Escrow STK Push..."
            when (val res = paymentRepository.initiateSTKPush(jobId, phoneNumber, paymentType)) {
                is Resource.Success -> {
                    _actionMessage.value = res.data?.message?.takeIf { it.isNotBlank() }
                        ?: "M-Pesa STK Push sent! Enter your PIN on your phone."
                    loadJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Payment failed: ${res.message}"
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
fun ClientMyJobsScreen(
    viewModel: ClientMyJobsViewModel = koinViewModel(),
    onViewApplicants: (Int) -> Unit = {},
    onOpenJobDetail: (Int) -> Unit = {}
) {
    val jobs by viewModel.jobs.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()
    val adjustmentsMap by viewModel.adjustmentsMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var jobToReview by remember { mutableStateOf<Job?>(null) }
    var jobToPay by remember { mutableStateOf<Job?>(null) }

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
                        text = "My Bookings & Escrow",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track Start PIN, Scope Adjustments & M-Pesa Escrow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.loadJobs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PremiumTealDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PremiumTealDark)
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
                            Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = PremiumTealDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No bookings yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap the Book tab to customize your Eldoret cleaning service!",
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
                        val progress = progressMap[job.id]
                        val adjustments = adjustmentsMap[job.id].orEmpty()
                        ClientJobCard(
                            job = job,
                            progress = progress,
                            adjustments = adjustments,
                            onOpenDetail = { onOpenJobDetail(job.id) },
                            onViewApplicants = { onViewApplicants(job.id) },
                            onAutoDispatch = { viewModel.autoDispatchJob(job.id) },
                            onDelete = { viewModel.deleteJob(job.id) },
                            onReviewRequest = { jobToReview = job },
                            onPayRequest = { jobToPay = job },
                            onRespondAdjustment = { adjId, action ->
                                viewModel.respondToScopeAdjustment(job.id, adjId, action)
                            }
                        )
                    }
                }
            }
        }
    }

    jobToReview?.let { job ->
        val progress = progressMap[job.id]
        val workerUserId = progress?.worker?.userId ?: progress?.worker?.id ?: job.workerId
        ReviewDialog(
            jobTitle = job.title,
            workerName = progress?.worker?.name,
            onDismiss = { jobToReview = null },
            onSubmitWithFavorite = { rating, comment, addToFavorites ->
                viewModel.verifyAndReviewJob(job.id, workerUserId, rating, comment, addToFavorites)
                jobToReview = null
            }
        )
    }

    jobToPay?.let { job ->
        val pendingApprovedAdj = adjustmentsMap[job.id].orEmpty().any { it.status == "approved" }
        PaymentDialog(
            job = job,
            hasScopeTopup = pendingApprovedAdj,
            onDismiss = { jobToPay = null },
            onPay = { phone, paymentType ->
                viewModel.initiatePayment(job.id, phone, paymentType)
                jobToPay = null
            }
        )
    }
}

@Composable
fun ClientJobCard(
    job: Job,
    progress: JobProgress?,
    adjustments: List<ScopeAdjustment>,
    onOpenDetail: () -> Unit,
    onViewApplicants: () -> Unit,
    onAutoDispatch: () -> Unit,
    onDelete: () -> Unit,
    onReviewRequest: () -> Unit,
    onPayRequest: () -> Unit,
    onRespondAdjustment: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val resolvedPin = progress?.startPin ?: job.startPin
    val pendingAdjustments = adjustments.filter { it.status.equals("pending", ignoreCase = true) }

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
                            tint = PremiumTealDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.estate ?: job.location,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (job.workerBringsDetergents) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = PremiumTealSurface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+Detergents",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PremiumTealDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                StatusBadge(status = job.status)
            }

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KSh ${job.budget.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = PremiumTealDark,
                    fontWeight = FontWeight.ExtraBold
                )
                val payStatus = (progress?.paymentStatus ?: job.paymentStatus ?: "unpaid").lowercase()
                val (payLabel, payBg, payFg) = when (payStatus) {
                    "escrow_held", "paid", "completed" -> Triple("Escrow Secured", SuccessGreen.copy(alpha = 0.14f), SuccessGreenDark)
                    "partial_escrow" -> Triple("50% Escrow Held", WarningAmber.copy(alpha = 0.16f), WarningAmberDark)
                    "released" -> Triple("Payout Released", PremiumTeal.copy(alpha = 0.14f), PremiumTealDark)
                    else -> Triple("Escrow Unpaid", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = payBg, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = payFg, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = payLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = payFg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4-Digit Handshake Start PIN Banner
            if (!resolvedPin.isNullOrBlank() && (job.status == JobStatus.ASSIGNED || job.status == JobStatus.ARRIVED)) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PremiumTealSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = PremiumTealDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gate Handshake Start PIN",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumTealDark
                                )
                            }
                            Text(
                                text = "Share this 4-digit PIN with your Mama Fua only after verifying scope at the door.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = PremiumTealDark,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = resolvedPin,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 4.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Assigned Mama Fua Info + Verified ID + Call Button
            progress?.worker?.let { worker ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PremiumTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = worker.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumTealDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = worker.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (worker.isIdVerified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = "Verified ID",
                                                tint = SuccessGreenDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (worker.isIdVerified) "National ID Verified" else "Assigned Mama Fua",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!worker.phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}"))
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (worker.badges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(worker.badges) { badge ->
                                    Surface(
                                        color = PremiumOrangeSurface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumOrangeDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Scope Adjustments Card
            if (pendingAdjustments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                pendingAdjustments.forEach { adj ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WarningAmberDark, RoundedCornerShape(14.dp)),
                        color = WarningAmber.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = WarningAmberDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "On-Site Scope Adjustment: +KSh ${adj.additionalAmount.toInt()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = adj.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onRespondAdjustment(adj.id, "reject") }) {
                                    Text("Decline", color = ErrorRedDark)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onRespondAdjustment(adj.id, "approve") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Approve +KSh ${adj.additionalAmount.toInt()}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val payStatus = (progress?.paymentStatus ?: job.paymentStatus ?: "unpaid").lowercase()
                val canFundEscrow = job.status != JobStatus.CANCELLED &&
                    job.status != JobStatus.COMPLETED &&
                    payStatus != "escrow_held" &&
                    payStatus != "released"

                if (canFundEscrow) {
                    Button(
                        onClick = onPayRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fund Escrow", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    TextButton(onClick = onOpenDetail) {
                        Text("Details", color = PremiumTealDark)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.status == JobStatus.OPEN) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = ErrorRedDark, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = onAutoDispatch,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PremiumOrangeDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Match", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onViewApplicants,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Applicants", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (job.status == JobStatus.COMPLETION_REQUESTED) {
                        Button(
                            onClick = onReviewRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Release Payout", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenDetail,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Track Progress", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: JobStatus) {
    val (color, text) = when (status) {
        JobStatus.OPEN -> Pair(PremiumTealDark, "Open")
        JobStatus.APPLIED -> Pair(PremiumBlueLight, "Applied")
        JobStatus.ACCEPTED, JobStatus.ASSIGNED -> Pair(WarningAmberDark, "Assigned")
        JobStatus.ARRIVED -> Pair(PremiumOrangeDark, "At Gate")
        JobStatus.IN_PROGRESS -> Pair(PremiumTeal, "Cleaning")
        JobStatus.COMPLETION_REQUESTED -> Pair(PremiumOrange, "Verify Scope")
        JobStatus.COMPLETED -> Pair(SuccessGreenDark, "Completed")
        JobStatus.DISPUTED -> Pair(ErrorRedDark, "Disputed")
        JobStatus.CANCELLED -> Pair(ErrorRed, "Cancelled")
    }

    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    ReviewDialog(
        jobTitle = null,
        workerName = null,
        onDismiss = onDismiss,
        onSubmitWithFavorite = { rating, comment, _ ->
            onSubmit(rating, comment)
        }
    )
}

@Composable
fun ReviewDialog(
    jobTitle: String? = null,
    workerName: String? = null,
    onDismiss: () -> Unit,
    onSubmitWithFavorite: (Int, String, Boolean) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var addToFavorites by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (jobTitle != null) "Confirm & Release Escrow" else "Rate Experience",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (jobTitle != null) {
                    Text(
                        text = "Confirm that $jobTitle is complete to release the M-Pesa escrow payout to ${workerName ?: "your Mama Fua"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= rating) WarningAmber else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Feedback / Review note") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (jobTitle != null && rating >= 4) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addToFavorites = !addToFavorites }
                    ) {
                        Checkbox(
                            checked = addToFavorites,
                            onCheckedChange = { addToFavorites = it }
                        )
                        Text(
                            text = "Add ${workerName ?: "this Mama Fua"} to My Favorites for 2-tap rebooking",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitWithFavorite(rating, comment, addToFavorites) },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark)
            ) {
                Text(if (jobTitle != null) "Confirm & Release" else "Submit")
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
    var reason by remember { mutableStateOf("Scope or quality disagreement") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Report Issue / Raise Dispute", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Filing a dispute pauses M-Pesa escrow auto-release while our Eldoret support team reviews.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Issue Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason.trim(), description.trim().ifBlank { reason.trim() }) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRedDark)
            ) {
                Text("Submit Dispute")
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
fun PaymentDialog(
    job: Job,
    hasScopeTopup: Boolean,
    onDismiss: () -> Unit,
    onPay: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("2547") }
    var paymentType by remember {
        mutableStateOf(if (hasScopeTopup) "scope_topup" else "full")
    }

    val fullAmount = job.budget.toInt()
    val depositAmount = kotlin.math.ceil(job.budget * 0.5).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "M-Pesa Escrow Checkout",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Funds are held safely in Mama Fua Escrow and only released when you confirm job completion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val options = listOf(
                    Triple("full", "100% Full Escrow (KSh $fullAmount)", "Recommended — secures full booking"),
                    Triple("deposit_50", "50% Commitment Deposit (KSh $depositAmount)", "Pay half now, balance upon completion"),
                    Triple("scope_topup", "On-Site Scope Top-Up", "Pay approved extra items added at gate")
                )

                options.forEach { (typeKey, title, subtitle) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { paymentType = typeKey },
                        color = if (paymentType == typeKey) PremiumTealSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentType == typeKey,
                                onClick = { paymentType = typeKey }
                            )
                            Column {
                                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("M-Pesa Phone (e.g. 254712345678)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPay(phone, paymentType) },
                enabled = phone.length >= 10,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark)
            ) {
                Text("Send STK Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
