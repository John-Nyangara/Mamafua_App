package com.mamafua.app.presentation.client

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientJobApplicantsScreen(
    jobId: Int,
    onBack: () -> Unit,
    onAccepted: (Int) -> Unit = { onBack() },
    viewModel: ClientJobApplicantsViewModel = koinViewModel(key = "applicants_$jobId") { parametersOf(jobId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage, uiState.error, uiState.acceptedWorker) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            if (uiState.acceptedWorker) {
                onAccepted(jobId)
            }
            viewModel.clearMessages()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Applicants",
            subtitle = "Review badges, ratings & accept a Mama Fua",
            onBackClick = onBack
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.applicants.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.GroupOff,
                title = "No Applicants Yet",
                subtitle = "Nearby Mama Fuas haven't applied yet. You can also broadcast Auto-Dispatch!",
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
                items(uiState.applicants, key = { it.resolvedUserId }) { worker ->
                    PremiumCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AvatarCircle(name = worker.displayName, size = 48.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = worker.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (worker.isIdVerified) {
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
                                        text = listOfNotNull(
                                            worker.estate ?: worker.location,
                                            if (worker.displayRate > 0) "KES ${worker.displayRate.toInt()}" else null
                                        ).joinToString(" • ").ifBlank { "Eldoret Mama Fua" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PremiumOrangeDark,
                                        fontWeight = FontWeight.SemiBold
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
                                    text = String.format("%.1f", worker.averageRating ?: 0.0),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (worker.badges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                worker.badges.forEach { badge ->
                                    Surface(shape = RoundedCornerShape(8.dp), color = PremiumOrangeSurface) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumOrangeDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        worker.bio?.takeIf { it.isNotBlank() }?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.acceptApplicant(worker.resolvedUserId) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = { viewModel.shortlistApplicant(worker.resolvedUserId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Shortlist")
                            }
                            OutlinedButton(
                                onClick = { viewModel.rejectApplicant(worker.resolvedUserId) },
                                modifier = Modifier.weight(0.9f)
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
