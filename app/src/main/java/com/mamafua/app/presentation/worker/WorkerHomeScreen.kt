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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkerHomeScreen(
    viewModel: WorkerHomeViewModel = koinViewModel(),
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val jobs = uiState.availableJobs
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "Available Jobs",
            subtitle = "${jobs.size} jobs near you",
            gradient = Brush_WorkerHeader,
            actions = {
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        // ── Content ──
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (jobs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.WorkOff,
                title = "No Jobs Available",
                subtitle = "Check back later for new opportunities near you",
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            // Title
                            Text(
                                text = job.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Description
                            Text(
                                text = job.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Budget + Location row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Budget badge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = PremiumOrangeDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KES ${job.budget}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumOrangeDark
                                    )
                                }

                                // Location
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Apply button
                            val hasApplied = uiState.appliedJobs.contains(job.id)
                            GradientButton(
                                text = if (hasApplied) "Applied" else "Apply for Job",
                                onClick = { if (!hasApplied) viewModel.applyForJob(job.id) },
                                gradient = if (hasApplied) 
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray)) 
                                else AccentGradient,
                                icon = if (hasApplied) Icons.Default.Check else Icons.Default.Send
                            )
                        }
                    }
                }
            }
        }
    }
}

// Worker-specific header gradient (warmer tones)
private val Brush_WorkerHeader = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(PremiumOrangeDark, PremiumOrange)
)
