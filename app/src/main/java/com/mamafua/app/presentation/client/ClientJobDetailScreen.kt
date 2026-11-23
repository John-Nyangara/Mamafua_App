package com.mamafua.app.presentation.client

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.PaymentRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

data class ClientJobDetailUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val job: Job? = null,
    val progress: JobProgress? = null,
    val scopeAdjustments: List<ScopeAdjustment> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val showConfirmAndRateModal: Boolean = false,
    val activePaymentId: Int? = null,
    val lastPaymentStatus: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ClientJobDetailViewModel(
    private val jobId: Int,
    private val jobRepository: JobRepository,
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientJobDetailUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    val currentUser = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshAll()
    }

    fun refreshAll(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }
            val jobRes = jobRepository.getJobById(jobId)
            val progressRes = jobRepository.getJobProgress(jobId)
            val scopeRes = jobRepository.getScopeAdjustments(jobId)
            val paymentsRes = paymentRepository.getJobPayments(jobId)
            val reviewsRes = jobRepository.getJobReviews(jobId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                job = (jobRes as? Resource.Success)?.data ?: _uiState.value.job,
                progress = (progressRes as? Resource.Success)?.data ?: _uiState.value.progress,
                scopeAdjustments = (scopeRes as? Resource.Success)?.data ?: _uiState.value.scopeAdjustments,
                payments = (paymentsRes as? Resource.Success)?.data ?: _uiState.value.payments,
                reviews = (reviewsRes as? Resource.Success)?.data ?: _uiState.value.reviews
            )
        }
    }

    fun initiateMpesaEscrow(
        phoneNumber: String,
        paymentType: String,
        autoTriggerDispatch: Boolean,
        onPromptSent: (paymentId: Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            when (val res = paymentRepository.initiateSTKPush(jobId, phoneNumber, paymentType)) {
                is Resource.Success -> {
                    val pId = res.data?.paymentId ?: 0
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        activePaymentId = pId,
                        lastPaymentStatus = "pending",
                        successMessage = "STK Push sent to phone!"
                    )
                    if (autoTriggerDispatch) {
                        jobRepository.autoDispatchJob(jobId, radiusKm = 5.0, maxBroadcast = 3)
                    }
                    onPromptSent(pId)
                    delay(1500)
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        errorMessage = res.message ?: "Failed to initiate M-Pesa STK Push"
                    )
                }
                else -> _uiState.value = _uiState.value.copy(isActionLoading = false)
            }
        }
    }

    fun queryPayment(paymentId: Int, onStatus: (String) -> Unit = {}) {
        viewModelScope.launch {
            when (val res = paymentRepository.checkPaymentStatus(paymentId)) {
                is Resource.Success -> {
                    val status = res.data?.status ?: "pending"
                    _uiState.value = _uiState.value.copy(lastPaymentStatus = status)
                    onStatus(status)
                    if (status == "paid") {
                        refreshAll(silent = true)
                    }
                }
                else -> Unit
            }
        }
    }

    fun triggerAutoDispatch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.autoDispatchJob(jobId, radiusKm = 5.0, maxBroadcast = 3)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = "Auto-Dispatch broadcast sent to top 3 nearby online Mama Fuas!"
                    )
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = res.message)
                }
                else -> _uiState.value = _uiState.value.copy(isActionLoading = false)
            }
        }
    }

    fun respondScopeAdjustment(adjustmentId: Int, action: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.respondToScopeAdjustment(jobId, adjustmentId, action)) {
                is Resource.Success -> {
                    val msg = if (action == "approve") {
                        "Scope adjustment approved! You can now top up the extra amount via M-Pesa."
                    } else {
                        "Scope adjustment rejected."
                    }
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = msg
                    )
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = res.message)
                }
                else -> _uiState.value = _uiState.value.copy(isActionLoading = false)
            }
        }
    }

    fun confirmJobCompletion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.confirmCompletion(jobId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        showConfirmAndRateModal = true,
                        successMessage = "Completion confirmed! M-Pesa B2C payout released to Mama Fua."
                    )
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = res.message)
                }
                else -> _uiState.value = _uiState.value.copy(isActionLoading = false)
            }
        }
    }

    fun submitReviewAndOptionalFavorite(
        rating: Int,
        comment: String,
        addToFavorites: Boolean,
        workerId: Int?
    ) {
        viewModelScope.launch {
            jobRepository.createReview(jobId, rating, comment.ifBlank { null })
            if (addToFavorites && workerId != null && workerId != 0) {
                userRepository.addFavorite(workerId, "5-star Mama Fua from Job #$jobId")
            }
            _uiState.value = _uiState.value.copy(
                showConfirmAndRateModal = false,
                successMessage = if (addToFavorites) "Review submitted & added to My Favorites!" else "Thank you for your review!"
            )
            refreshAll(silent = true)
        }
    }

    fun dismissRateModal() {
        _uiState.value = _uiState.value.copy(showConfirmAndRateModal = false)
    }

    fun openRateModal() {
        _uiState.value = _uiState.value.copy(showConfirmAndRateModal = true)
    }

    fun raiseDispute(reason: String, description: String) {
        viewModelScope.launch {
            when (val res = jobRepository.raiseDispute(jobId, reason, description)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Dispute filed. Escrow is frozen pending review.")
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> Unit
            }
        }
    }

    fun cancelJob(onCancelled: () -> Unit) {
        viewModelScope.launch {
            when (val res = jobRepository.cancelJob(jobId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Booking cancelled.")
                    onCancelled()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}

@Composable
fun ClientJobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
    onNavigateToApplicants: (Int) -> Unit,
    viewModel: ClientJobDetailViewModel = koinViewModel(key = "client_job_$jobId") { parametersOf(jobId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var showEscrowSheet by remember { mutableStateOf<String?>(null) } // "full", "deposit_50", "scope_topup"
    var showDisputeDialog by remember { mutableStateOf(false) }

    // Poll job progress every 12 seconds while screen is open and job is active
    LaunchedEffect(jobId) {
        while (true) {
            delay(12_000)
            val status = uiState.progress?.status ?: uiState.job?.status
            if (status != JobStatus.COMPLETED && status != JobStatus.CANCELLED) {
                viewModel.refreshAll(silent = true)
            }
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    val job = uiState.job
    val progress = uiState.progress
    val effectiveStatus = progress?.status ?: job?.status ?: JobStatus.OPEN
    val effectiveBudget = progress?.budget ?: job?.budget ?: 0.0
    val effectivePaymentStatus = progress?.paymentStatus ?: job?.paymentStatus ?: "unpaid"
    val startPin = progress?.startPin ?: job?.startPin
    val assignedWorker = progress?.worker
    val resolvedWorkerId = assignedWorker?.userId ?: assignedWorker?.id ?: job?.workerId

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Booking #$jobId",
            subtitle = effectiveStatus.displayLabel,
            onBackClick = onBack,
            actions = {
                IconButton(onClick = { viewModel.refreshAll() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        )

        if (uiState.isLoading && job == null && progress == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val bannerUrl = EldoretCatalogDefaults.getCategoryImage(job?.category)

                AsyncImage(
                    model = bannerUrl,
                    contentDescription = job?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 1. HIGH-CONTRAST 4-DIGIT START PIN BADGE (Handshake) ──
                    if (!startPin.isNullOrBlank() && effectiveStatus in listOf(
                            JobStatus.ASSIGNED,
                            JobStatus.ACCEPTED,
                            JobStatus.ARRIVED
                        )
                    ) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = PremiumBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = PremiumOrangeLight
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GATE / DOOR HANDSHAKE PIN",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumOrangeLight
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    startPin.trim().forEach { digit ->
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit.toString(),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumBlue
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Give this 4-digit Start PIN to your Mama Fua at the door to officially start the timer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // ── 2. URGENT COMPLETION CONFIRMATION BANNER ──
                    if (effectiveStatus == JobStatus.COMPLETION_REQUESTED) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreenSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, SuccessGreenDark, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Mama Fua Marked Cleaning Complete!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreenDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Inspect your laundry/home and confirm satisfaction to release M-Pesa payout.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                GradientButton(
                                    text = "Confirm Satisfaction & Release Payout",
                                    onClick = { viewModel.confirmJobCompletion() },
                                    gradient = mpesaGradient(),
                                    isLoading = uiState.isActionLoading,
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                        }
                    }

                    // ── 3. ON-SITE SCOPE ADJUSTMENT REQUESTS ──
                    val (pinnedJobLat, pinnedJobLng) = EldoretCatalogDefaults.resolveCoordinates(
                        latitude = progress?.latitude ?: job?.latitude,
                        longitude = progress?.longitude ?: job?.longitude,
                        estateName = progress?.estate ?: job?.estate,
                        locationText = progress?.location ?: job?.location
                    )
                    OsmLocationPickerCard(
                        title = "Pinned Gate on OpenStreetMap",
                        subtitle = "Exact coordinates shared with your Mama Fua",
                        latitude = pinnedJobLat,
                        longitude = pinnedJobLng,
                        selectedEstateName = progress?.estate ?: job?.estate,
                        addressLabel = progress?.location ?: job?.location ?: "Eldoret",
                        readOnly = true,
                        mapHeight = 190.dp,
                        accentColor = PremiumTealDark
                    )

                    if (uiState.scopeAdjustments.isNotEmpty()) {
                        uiState.scopeAdjustments.forEach { adj ->
                            val isPending = adj.status.equals("pending", ignoreCase = true)
                            val isApproved = adj.status.equals("approved", ignoreCase = true)
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPending) PremiumOrangeSurface else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isPending) PremiumOrangeDark else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        ),
                                        RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "On-Site Scope Adjustment",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        StatusChip(
                                            text = adj.status.uppercase(),
                                            color = when {
                                                isPending -> WarningAmberDark
                                                isApproved -> SuccessGreenDark
                                                else -> ErrorRedDark
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "+KES ${adj.additionalAmount.toInt()} • ${adj.reason}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PremiumOrangeDark
                                    )
                                    adj.extraItems.forEach { extra ->
                                        Text(
                                            text = "• ${extra.quantity}x ${extra.name} (KES ${extra.amount.toInt()})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isPending) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.respondScopeAdjustment(
                                                        adj.id,
                                                        "approve"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve (+${adj.additionalAmount.toInt()})")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.respondScopeAdjustment(
                                                        adj.id,
                                                        "reject"
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject", color = ErrorRedDark)
                                            }
                                        }
                                    } else if (isApproved) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { showEscrowSheet = "scope_topup" },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = MpesaGreenDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Top Up +KES ${adj.additionalAmount.toInt()} via M-Pesa",
                                                color = MpesaGreenDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 4. BOOKING SUMMARY & 4-STEP CHECKLIST ──
                    PremiumCard {
                        Text(
                            text = progress?.title ?: job?.title ?: "Cleaning Booking #$jobId",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!job?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = job?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "KES ${effectiveBudget.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PremiumOrangeDark
                            )
                            Text(
                                text = progress?.location ?: job?.location ?: job?.estate
                                ?: "Eldoret",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        Text(
                            text = "Live Job Checklist",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val steps = progress?.checklistSteps ?: ChecklistSteps(
                            assigned = effectiveStatus != JobStatus.OPEN && effectiveStatus != JobStatus.CANCELLED,
                            arrived = effectiveStatus in listOf(
                                JobStatus.ARRIVED,
                                JobStatus.IN_PROGRESS,
                                JobStatus.COMPLETION_REQUESTED,
                                JobStatus.COMPLETED
                            ),
                            startedCleaning = effectiveStatus in listOf(
                                JobStatus.IN_PROGRESS,
                                JobStatus.COMPLETION_REQUESTED,
                                JobStatus.COMPLETED
                            ),
                            completed = effectiveStatus == JobStatus.COMPLETED
                        )
                        ChecklistStepRow("1. Mama Fua Assigned & Locked", steps.assigned)
                        ChecklistStepRow("2. Arrived at Gate / Door", steps.arrived)
                        ChecklistStepRow(
                            "3. Start PIN Verified • Cleaning Started",
                            steps.startedCleaning
                        )
                        ChecklistStepRow("4. Completed & Escrow Released", steps.completed)
                    }

                    // ── 5. ASSIGNED MAMA FUA CARD + CALL BUTTON ──
                    if (assignedWorker != null) {
                        PremiumCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarCircle(name = assignedWorker.name, size = 48.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = assignedWorker.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (assignedWorker.isIdVerified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = "ID Verified",
                                                    tint = PremiumTealDark,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (assignedWorker.badges.isNotEmpty())
                                                assignedWorker.badges.joinToString(" • ")
                                            else "Assigned Mama Fua",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!assignedWorker.phone.isNullOrBlank()) {
                                    FilledIconButton(
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${assignedWorker.phone}")
                                            )
                                            context.startActivity(intent)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = SuccessGreenDark
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = "Call Mama Fua",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 6. M-PESA ESCROW & AUTO-DISPATCH CONTROLS ──
                    PremiumCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "M-Pesa Escrow Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusChip(
                                text = effectivePaymentStatus.uppercase(),
                                color = when (effectivePaymentStatus.lowercase()) {
                                    "held", "completed", "released", "paid" -> SuccessGreenDark
                                    "pending" -> WarningAmberDark
                                    else -> PremiumOrangeDark
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Funds are held safely in Mama Fua Escrow and only released to the worker after you confirm satisfaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (effectivePaymentStatus.lowercase() !in listOf(
                                "held",
                                "completed",
                                "released"
                            ) &&
                            effectiveStatus != JobStatus.CANCELLED
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showEscrowSheet = "full" },
                                    colors = ButtonDefaults.buttonColors(containerColor = MpesaGreenDark),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pay 100% Escrow")
                                }
                                OutlinedButton(
                                    onClick = { showEscrowSheet = "deposit_50" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("50% Deposit", color = MpesaGreenDark)
                                }
                            }
                        }

                        if (uiState.payments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.payments.take(3).forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "KES ${p.amount.toInt()} • ${p.status}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    TextButton(onClick = { viewModel.queryPayment(p.id) }) {
                                        Text(
                                            "Check Status",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        if (effectiveStatus == JobStatus.OPEN) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.triggerAutoDispatch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Radar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Broadcast Radar")
                                }
                                OutlinedButton(
                                    onClick = { onNavigateToApplicants(jobId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Applicants")
                                }
                            }
                        }
                    }

                    // ── 7. BOTTOM ACTIONS (Confirm / Rate / Cancel / Dispute) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (effectiveStatus == JobStatus.IN_PROGRESS) {
                            Button(
                                onClick = { viewModel.confirmJobCompletion() },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm Complete")
                            }
                        }
                        if (effectiveStatus == JobStatus.COMPLETED && uiState.reviews.isEmpty()) {
                            Button(
                                onClick = { viewModel.openRateModal() },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rate & Favorite")
                            }
                        }
                        if (effectiveStatus == JobStatus.OPEN) {
                            OutlinedButton(
                                onClick = { viewModel.cancelJob(onBack) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel Booking", color = ErrorRedDark)
                            }
                        } else if (effectiveStatus !in listOf(
                                JobStatus.CANCELLED,
                                JobStatus.DISPUTED
                            )
                        ) {
                            OutlinedButton(
                                onClick = { showDisputeDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("File Dispute", color = ErrorRedDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Live Interactive M-Pesa Escrow STK Push Modal
        showEscrowSheet?.let { initialType ->
            var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
            var paymentType by remember { mutableStateOf(initialType) }
            var autoDispatchAfter by remember {
                mutableStateOf(
                    (job?.matchingMode
                        ?: "auto_dispatch") == "auto_dispatch" && effectiveStatus == JobStatus.OPEN
                )
            }
            var currentStage by remember { mutableStateOf("SETUP") } // "SETUP", "POLLING", "SUCCESS", "FAILED"
            var activePaymentId by remember { mutableIntStateOf(0) }
            var countdownSeconds by remember { mutableIntStateOf(60) }

            val targetAmount = when (paymentType) {
                "deposit_50" -> (effectiveBudget * 0.5).toInt()
                "scope_topup" -> {
                    val totalPaid =
                        uiState.payments.filter { it.status == "paid" }.sumOf { it.amount }
                    Math.max(0, (effectiveBudget - totalPaid).toInt())
                }

                else -> {
                    val totalPaid =
                        uiState.payments.filter { it.status == "paid" }.sumOf { it.amount }
                    if (totalPaid > 0) Math.max(
                        0,
                        (effectiveBudget - totalPaid).toInt()
                    ) else effectiveBudget.toInt()
                }
            }

            // Live 60-Second Countdown & 3-Second Status Poller while in POLLING stage
            LaunchedEffect(currentStage, activePaymentId) {
                if (currentStage == "POLLING" && activePaymentId > 0) {
                    countdownSeconds = 60
                    while (countdownSeconds > 0 && currentStage == "POLLING") {
                        delay(1000)
                        countdownSeconds--
                        if (countdownSeconds % 3 == 0) {
                            viewModel.queryPayment(activePaymentId) { status ->
                                if (status == "paid") {
                                    currentStage = "SUCCESS"
                                } else if (status == "failed") {
                                    currentStage = "FAILED"
                                }
                            }
                        }
                    }
                    if (countdownSeconds <= 0 && currentStage == "POLLING") {
                        currentStage = "FAILED"
                    }
                } else if (currentStage == "SUCCESS") {
                    delay(2000)
                    showEscrowSheet = null
                }
            }

            AlertDialog(
                onDismissRequest = {
                    if (currentStage != "POLLING") showEscrowSheet = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (currentStage) {
                                "SUCCESS" -> Icons.Default.CheckCircle
                                "FAILED" -> Icons.Default.Error
                                else -> Icons.Default.Payments
                            },
                            contentDescription = null,
                            tint = when (currentStage) {
                                "SUCCESS" -> SuccessGreenDark
                                "FAILED" -> ErrorRedDark
                                else -> MpesaGreenDark
                            },
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentStage) {
                                "POLLING" -> "Waiting for M-Pesa PIN"
                                "SUCCESS" -> "Escrow Confirmed!"
                                "FAILED" -> "Payment Timed Out"
                                else -> "M-Pesa Escrow Payment"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (currentStage) {
                                "SUCCESS" -> SuccessGreenDark
                                "FAILED" -> ErrorRedDark
                                else -> MpesaGreenDark
                            }
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (currentStage) {
                            "SETUP" -> {
                                Text(
                                    text = "Your money is held in escrow and only disbursed after you confirm job completion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = paymentType == "full",
                                        onClick = { paymentType = "full" },
                                        label = { Text("100% Full") }
                                    )
                                    FilterChip(
                                        selected = paymentType == "deposit_50",
                                        onClick = { paymentType = "deposit_50" },
                                        label = { Text("50% Deposit") }
                                    )
                                    FilterChip(
                                        selected = paymentType == "scope_topup",
                                        onClick = { paymentType = "scope_topup" },
                                        label = { Text("Top-Up") }
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PremiumTealSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Amount to Pay:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "KES $targetAmount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumTealDark
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("M-Pesa Phone Number") },
                                    placeholder = { Text("0712345678 or 2547...") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = MpesaGreenDark
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (effectiveStatus == JobStatus.OPEN) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = autoDispatchAfter,
                                            onCheckedChange = { autoDispatchAfter = it })
                                        Text(
                                            text = "Ping top 3 nearby Mama Fuas immediately",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            "POLLING" -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(72.dp),
                                        strokeWidth = 4.dp,
                                        color = MpesaGreenDark
                                    )
                                    Text(
                                        text = "${countdownSeconds}s",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MpesaGreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "STK Prompt sent to $phone",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Please enter your M-Pesa PIN on your phone to authorize KES $targetAmount into Escrow.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            "SUCCESS" -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreenDark,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "Payment Received!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreenDark
                                )
                                Text(
                                    text = "KES $targetAmount is now secured in escrow. Your Mama Fua booking is officially locked!",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            "FAILED" -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRedDark,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Payment Incomplete",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRedDark
                                )
                                Text(
                                    text = "The M-Pesa prompt was either cancelled or timed out. Please check your phone balance and try again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    when (currentStage) {
                        "SETUP" -> {
                            Button(
                                onClick = {
                                    if (phone.isNotBlank()) {
                                        viewModel.initiateMpesaEscrow(
                                            phone,
                                            paymentType,
                                            autoDispatchAfter
                                        ) { pId ->
                                            activePaymentId = pId
                                            currentStage = "POLLING"
                                        }
                                    }
                                },
                                enabled = !uiState.isActionLoading && phone.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MpesaGreenDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isActionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sending STK...")
                                } else {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send M-Pesa Prompt")
                                }
                            }
                        }

                        "POLLING" -> {
                            Button(
                                onClick = {
                                    if (activePaymentId > 0) {
                                        viewModel.queryPayment(activePaymentId) { status ->
                                            if (status == "paid") currentStage = "SUCCESS"
                                            else if (status == "failed") currentStage = "FAILED"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MpesaGreenDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("I Entered My PIN — Check Status")
                            }
                        }

                        "FAILED" -> {
                            Button(
                                onClick = { currentStage = "SETUP" },
                                colors = ButtonDefaults.buttonColors(containerColor = MpesaGreenDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Try Again")
                            }
                        }

                        else -> Unit
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEscrowSheet = null }
                    ) {
                        Text(if (currentStage == "SUCCESS") "Done" else "Cancel")
                    }
                }
            )
        }

        // Confirm & Rate + Add to My Favorites Modal
        if (uiState.showConfirmAndRateModal) {
            var rating by remember { mutableStateOf(5) }
            var comment by remember { mutableStateOf("") }
            var addToFavorites by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { viewModel.dismissRateModal() },
                title = { Text("Rate Your Mama Fua", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "How was the cleaning & laundry service?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = {
                                        rating = i
                                        if (i < 5) addToFavorites = false else addToFavorites = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star $i",
                                        tint = if (i <= rating) PremiumOrange else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Review comment (e.g. Spotless kitchen & fast ironing!)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (rating == 5 && resolvedWorkerId != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PremiumOrangeSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = addToFavorites,
                                        onCheckedChange = { addToFavorites = it }
                                    )
                                    Text(
                                        text = "Add to My Favorite Mama Fuas for 2-Tap Rebooking",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitReviewAndOptionalFavorite(
                                rating = rating,
                                comment = comment,
                                addToFavorites = addToFavorites && rating == 5,
                                workerId = resolvedWorkerId
                            )
                        }
                    ) {
                        Text("Submit Review")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissRateModal() }) {
                        Text("Skip")
                    }
                }
            )
        }

        if (showDisputeDialog) {
            DisputeDialog(
                onDismiss = { showDisputeDialog = false },
                onSubmit = { reason, description ->
                    viewModel.raiseDispute(reason, description)
                    showDisputeDialog = false
                }
            )
        }
    }
}


@Composable
fun ChecklistStepRow(label: String, done: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) SuccessGreenDark else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (done) Icons.Default.Check else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
