package com.mamafua.app.presentation.worker

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.repository.CatalogRepository
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.presentation.client.DisputeDialog
import com.mamafua.app.presentation.client.ReviewDialog
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

data class WorkerJobDetailUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val job: Job? = null,
    val progress: JobProgress? = null,
    val scopeAdjustments: List<ScopeAdjustment> = emptyList(),
    val catalog: CatalogData = EldoretCatalogDefaults.defaultCatalog(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class WorkerJobDetailViewModel(
    private val jobId: Int,
    private val jobRepository: JobRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerJobDetailUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }
            val catalog = catalogRepository.getCatalog()
            val jobRes = jobRepository.getJobById(jobId)
            val progressRes = jobRepository.getJobProgress(jobId)
            val scopeRes = jobRepository.getScopeAdjustments(jobId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                catalog = catalog,
                job = (jobRes as? Resource.Success)?.data ?: _uiState.value.job,
                progress = (progressRes as? Resource.Success)?.data ?: _uiState.value.progress,
                scopeAdjustments = (scopeRes as? Resource.Success)?.data ?: _uiState.value.scopeAdjustments
            )
        }
    }

    // Step 1: Tap "Arrived at Gate / Door"
    fun markArrivedAtGate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.updateJobStatus(jobId, JobStatus.ARRIVED)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = "Client notified you arrived! Inspect scope or enter 4-digit Start PIN."
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

    // Step 2: Optional On-Site Scope Adjustment before touching water
    fun requestScopeAdjustment(
        additionalAmount: Double,
        reason: String,
        extraItems: List<ScopeExtraItem>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.createScopeAdjustment(jobId, additionalAmount, reason, extraItems)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = "Scope adjustment (+KES ${additionalAmount.toInt()}) sent to client for approval!"
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

    // Step 3: Enter Client's 4-Digit Start PIN
    fun verifyStartPin(pin: String) {
        if (pin.trim().length < 4) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter the client's 4-digit Start PIN")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.verifyStartPin(jobId, pin.trim())) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = "Start PIN verified! Cleaning timer started."
                    )
                    refreshAll(silent = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        errorMessage = res.message ?: "Incorrect Start PIN"
                    )
                }
                else -> _uiState.value = _uiState.value.copy(isActionLoading = false)
            }
        }
    }

    // Step 4: Mark Job Complete
    fun markCleaningComplete() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            when (val res = jobRepository.markJobCompleteByWorker(jobId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        successMessage = "Marked complete! Waiting for client to confirm & release M-Pesa payout."
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

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch {
            when (val res = jobRepository.createReview(jobId, rating, comment.ifBlank { null })) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(successMessage = "Review submitted!")
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                else -> Unit
            }
        }
    }

    fun raiseDispute(reason: String, description: String) {
        viewModelScope.launch {
            when (val res = jobRepository.raiseDispute(jobId, reason, description)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Dispute submitted to support.")
                    refreshAll(silent = true)
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                else -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}

@Composable
fun WorkerJobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
    viewModel: WorkerJobDetailViewModel = koinViewModel(key = "worker_job_$jobId") { parametersOf(jobId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var pinInput by remember { mutableStateOf("") }
    var showScopeModal by remember { mutableStateOf(false) }
    var showDisputeModal by remember { mutableStateOf(false) }
    var showReviewModal by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        while (true) {
            delay(12_000)
            val st = uiState.progress?.status ?: uiState.job?.status
            if (st != JobStatus.COMPLETED && st != JobStatus.CANCELLED) {
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
    val effectiveStatus = progress?.status ?: job?.status ?: JobStatus.ASSIGNED
    val effectiveBudget = progress?.budget ?: job?.budget ?: 0.0
    val netPayout = (effectiveBudget * 0.9).toInt()
    val locationStr = progress?.location ?: job?.location ?: job?.estate ?: "Eldoret"

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Active Job #$jobId",
            subtitle = effectiveStatus.displayLabel,
            gradient = Brush.verticalGradient(listOf(PremiumOrangeDark, PremiumOrange)),
            onBackClick = onBack,
            actions = {
                IconButton(onClick = { viewModel.refreshAll() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        )

        if (uiState.isLoading && job == null && progress == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumOrangeDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── JOB HEADER & NAVIGATION / CALL ACTIONS ──
                PremiumCard {
                    Text(
                        text = progress?.title ?: job?.title ?: "Assigned Cleaning #$jobId",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your Net Payout (90%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "KES $netPayout",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreenDark
                            )
                            Text(
                                text = "Total Job Budget: KES ${effectiveBudget.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(
                            text = (progress?.paymentStatus ?: job?.paymentStatus ?: "escrow").uppercase(),
                            color = SuccessGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = locationStr,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    val (jobLat, jobLng) = EldoretCatalogDefaults.resolveCoordinates(
                        latitude = progress?.latitude ?: job?.latitude,
                        longitude = progress?.longitude ?: job?.longitude,
                        estateName = progress?.estate ?: job?.estate,
                        locationText = locationStr
                    )
                    OsmLocationPickerCard(
                        title = "Client Gate Pin (OpenStreetMap)",
                        subtitle = "Exact pinned location in ${progress?.estate ?: job?.estate ?: "Eldoret"}",
                        latitude = jobLat,
                        longitude = jobLng,
                        selectedEstateName = progress?.estate ?: job?.estate,
                        addressLabel = locationStr,
                        readOnly = true,
                        mapHeight = 190.dp,
                        accentColor = PremiumOrangeDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("google.navigation:q=$jobLat,$jobLng")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    val geoUri = Uri.parse("geo:$jobLat,$jobLng?q=$jobLat,$jobLng(${Uri.encode(locationStr)})")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate")
                        }
                        progress?.client?.phone?.takeIf { it.isNotBlank() }?.let { clientPhone ->
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clientPhone")))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Client")
                            }
                        }
                    }
                }

                // ── STEP-BY-STEP WORKER EXECUTION CHECKLIST (Screen 3.3) ──
                PremiumCard {
                    Text(
                        text = "Mama Fua Trust & Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // STEP 1: ARRIVED AT GATE / DOOR
                    if (effectiveStatus == JobStatus.ASSIGNED || effectiveStatus == JobStatus.ACCEPTED) {
                        Text(
                            text = "Step 1: Tap below once you arrive at the client's gate or apartment door.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GradientButton(
                            text = "Step 1: I Have Arrived at Gate / Door",
                            onClick = { viewModel.markArrivedAtGate() },
                            gradient = AccentGradient,
                            isLoading = uiState.isActionLoading,
                            icon = Icons.Default.DoorFront
                        )
                    }

                    // STEP 2 & 3: ON-SITE SCOPE VERIFICATION + 4-DIGIT START PIN
                    if (effectiveStatus in listOf(JobStatus.ASSIGNED, JobStatus.ACCEPTED, JobStatus.ARRIVED)) {
                        if (effectiveStatus == JobStatus.ARRIVED) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreenSurface,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "✓ Step 1 Done: Client notified you are at the door.",
                                    color = SuccessGreenDark,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                        }

                        // Step 2: Optional On-Site Scope Verification
                        OutlinedButton(
                            onClick = { showScopeModal = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = PremiumOrangeDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Step 2 (Optional): Request Extra KSh Scope Adjustment",
                                color = PremiumOrangeDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Step 3: Enter Client's 4-Digit Start PIN
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PremiumTealSurface.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Step 3: Enter Client's 4-Digit Start PIN",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ask the client for their 4-digit Start PIN before touching water to start the job timer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 4) {
                                        val digit = pinInput.getOrNull(i)?.toString() ?: ""
                                        val isCurrent = pinInput.length == i
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (digit.isNotEmpty()) PremiumTealSurface else MaterialTheme.colorScheme.surface
                                                )
                                                .border(
                                                    width = if (isCurrent) 2.dp else 1.dp,
                                                    color = if (isCurrent) PremiumTealDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumTealDark
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                                    label = { Text("Tap here to enter client's 4 digits") },
                                    placeholder = { Text("e.g. 4829") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.verifyStartPin(pinInput) },
                                    enabled = pinInput.length == 4 && !uiState.isActionLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify PIN & Start Cleaning Timer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // STEP 4: IN PROGRESS -> MARK COMPLETE
                    if (effectiveStatus == JobStatus.IN_PROGRESS) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WarningAmber.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Cleaning in Progress (Start PIN Verified)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumOrangeDark
                                )
                                progress?.startedAt?.let { started ->
                                    Text(
                                        text = "Started at: $started",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientButton(
                            text = "Step 4: Mark Cleaning Complete",
                            onClick = { viewModel.markCleaningComplete() },
                            gradient = mpesaGradient(),
                            isLoading = uiState.isActionLoading,
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    // COMPLETION REQUESTED
                    if (effectiveStatus == JobStatus.COMPLETION_REQUESTED) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SuccessGreenSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⏳ Awaiting Client Confirmation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreenDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "The client has been prompted to confirm completion and auto-release your M-Pesa payout!",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // COMPLETED
                    if (effectiveStatus == JobStatus.COMPLETED) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SuccessGreenSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Job Completed & Payout Credited!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreenDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { showReviewModal = true }) {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Rate Client")
                                }
                            }
                        }
                    }
                }

                // ── EXISTING SCOPE ADJUSTMENTS LIST ──
                if (uiState.scopeAdjustments.isNotEmpty()) {
                    PremiumCard {
                        Text(
                            text = "Scope Adjustment Requests",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.scopeAdjustments.forEach { adj ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("+KES ${adj.additionalAmount.toInt()}", fontWeight = FontWeight.Bold)
                                    Text(adj.reason, style = MaterialTheme.typography.bodySmall)
                                }
                                StatusChip(
                                    text = adj.status.uppercase(),
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

                // Raise Dispute Button
                if (effectiveStatus !in listOf(JobStatus.CANCELLED, JobStatus.DISPUTED)) {
                    OutlinedButton(
                        onClick = { showDisputeModal = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Report Issue / Raise Dispute", color = ErrorRedDark)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Scope Adjustment Request Modal (Before Touching Water)
    if (showScopeModal) {
        var selectedExtraCode by remember { mutableStateOf(uiState.catalog.services.firstOrNull()?.code ?: "laundry_duvet_heavy") }
        var extraQty by remember { mutableStateOf(1) }
        var customAmount by remember { mutableStateOf("450") }
        var reason by remember { mutableStateOf("Client added extra laundry/duvet not in original booking") }

        val selectedCatalogItem = uiState.catalog.services.find { it.code == selectedExtraCode }

        AlertDialog(
            onDismissRequest = { showScopeModal = false },
            title = { Text("On-Site Scope Verification", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Inspect the laundry pile before touching water. Quick-pick an extra item or enter the additional KES:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    uiState.catalog.services.take(5).forEach { svc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedExtraCode == svc.code,
                                onClick = {
                                    selectedExtraCode = svc.code
                                    customAmount = (svc.basePrice * extraQty).toInt().toString()
                                }
                            )
                            Text(
                                text = "${svc.name} (+KES ${svc.basePrice.toInt()})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        label = { Text("Additional Amount (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason shown to client") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = customAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && reason.isNotBlank()) {
                            val extras = if (selectedCatalogItem != null) {
                                listOf(
                                    ScopeExtraItem(
                                        code = selectedCatalogItem.code,
                                        name = selectedCatalogItem.name,
                                        quantity = extraQty,
                                        amount = amt
                                    )
                                )
                            } else emptyList()
                            viewModel.requestScopeAdjustment(amt, reason, extras)
                            showScopeModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark)
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScopeModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReviewModal) {
        ReviewDialog(
            onDismiss = { showReviewModal = false },
            onSubmit = { rating, comment ->
                viewModel.submitReview(rating, comment)
                showReviewModal = false
            }
        )
    }

    if (showDisputeModal) {
        DisputeDialog(
            onDismiss = { showDisputeModal = false },
            onSubmit = { reason, description ->
                viewModel.raiseDispute(reason, description)
                showDisputeModal = false
            }
        )
    }
}
