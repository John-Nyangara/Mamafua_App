package com.mamafua.app.presentation.client

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ClientJobApplicantsScreen(
    jobId: Int,
    onBack: () -> Unit,
    viewModel: ClientJobApplicantsViewModel = koinViewModel { parametersOf(jobId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage, uiState.error, uiState.acceptedWorker) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            if (uiState.acceptedWorker) {
                onBack()
            }
            viewModel.clearMessages()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header with Back Button ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Applicants",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Review and accept a worker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Content ──
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.applicants.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.GroupOff,
                title = "No Applicants Yet",
                subtitle = "Workers haven't applied to this job yet.",
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
                itemsIndexed(uiState.applicants) { index, worker ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarCircle(
                                        name = "Worker ${worker.userId}",
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Worker ${worker.userId}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "KES ${worker.hourlyRate}/hr",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PremiumOrangeDark
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = PremiumOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${worker.averageRating}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            worker.bio?.let {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GradientButton(
                                    text = "Accept",
                                    onClick = { viewModel.acceptApplicant(worker.resolvedUserId) },
                                    gradient = AccentGradient,
                                    icon = Icons.Default.CheckCircle,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { viewModel.rejectApplicant(worker.resolvedUserId) },
                                    modifier = Modifier.height(56.dp).weight(1f)
                                ) {
                                    Text("Reject", color = ErrorRedDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
