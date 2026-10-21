package com.mamafua.app.presentation.worker

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.mamafua.app.presentation.client.ReviewDialog
import com.mamafua.app.presentation.client.DisputeDialog

class WorkerMyJobsViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {
    val jobs = jobRepository.getActiveJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                jobRepository.fetchMyAssignedJobs()
                jobRepository.fetchMyApplications()
            } catch (_: Exception) {}
        }
    }

    private fun fetchJobs() {
        viewModelScope.launch {
            try {
                jobRepository.fetchMyAssignedJobs()
                jobRepository.fetchMyApplications()
            } catch (_: Exception) {}
        }
    }

    fun updateJobStatus(jobId: Int, status: com.mamafua.app.domain.model.JobStatus) {
        viewModelScope.launch {
            try {
                jobRepository.updateJobStatus(jobId, status)
                fetchJobs()
            } catch (e: Exception) {}
        }
    }

    fun createReview(jobId: Int, rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                jobRepository.createReview(jobId, rating, comment)
            } catch (e: Exception) {}
        }
    }

    fun raiseDispute(jobId: Int, reason: String, description: String) {
        viewModelScope.launch {
            try {
                jobRepository.raiseDispute(jobId, reason, description)
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun WorkerMyJobsScreen(
    viewModel: WorkerMyJobsViewModel = koinViewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    
    var jobToDispute by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "My Assigned Jobs",
            subtitle = "${jobs.size} active assignments",
            gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(PremiumOrangeDark, PremiumOrange)
            )
        )

        // ── Content ──
        if (jobs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Assignment,
                title = "No Assigned Jobs",
                subtitle = "Apply for available jobs to get started",
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
                            
                            if (job.status == com.mamafua.app.domain.model.JobStatus.ASSIGNED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GradientButton(
                                        text = "Start Job",
                                        onClick = { viewModel.updateJobStatus(job.id, com.mamafua.app.domain.model.JobStatus.IN_PROGRESS) },
                                        gradient = PrimaryGradientHorizontal,
                                        icon = Icons.Default.PlayArrow,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { jobToDispute = job.id },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text("Dispute", color = ErrorRedDark)
                                    }
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.IN_PROGRESS) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GradientButton(
                                        text = "Mark Completed",
                                        onClick = { viewModel.updateJobStatus(job.id, com.mamafua.app.domain.model.JobStatus.COMPLETED) },
                                        gradient = PrimaryGradientHorizontal,
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { jobToDispute = job.id },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text("Raise Dispute", color = ErrorRedDark)
                                    }
                                }
                            } else if (job.status == com.mamafua.app.domain.model.JobStatus.COMPLETED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Job completed successfully.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SuccessGreenDark,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
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
    


    jobToDispute?.let { jobId ->
        DisputeDialog(
            onDismiss = { jobToDispute = null },
            onSubmit = { reason, description ->
                viewModel.raiseDispute(jobId, reason, description)
                jobToDispute = null
            }
        )
    }
}
