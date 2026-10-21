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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import org.koin.androidx.compose.koinViewModel

@Composable
fun ClientHomeScreen(
    viewModel: ClientHomeViewModel = koinViewModel(),
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val workers = uiState.workers

    LaunchedEffect(Unit) {
        viewModel.searchWorkers(-1.2921, 36.8219)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "Hello, Homeowner",
            subtitle = "Find trusted workers near you",
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
        } else if (workers.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "No Workers Found Nearby",
                subtitle = "Try adjusting your search area or check back later",
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
                itemsIndexed(workers) { index, worker ->
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
                            // Top row: Avatar + Name + Rating
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

                                // Rating
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

                            // Bio
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

                            // Bottom row: Status + View Profile button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PulsingDot(
                                        color = if (worker.availabilityStatus == "AVAILABLE")
                                            StatusAvailable else StatusOffline,
                                        size = 8.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusChip(
                                        text = worker.availabilityStatus ?: "UNKNOWN",
                                        color = if (worker.availabilityStatus == "AVAILABLE")
                                            StatusAvailable else StatusOffline
                                    )
                                }

                                GradientButton(
                                    text = "View Profile",
                                    onClick = { Toast.makeText(context, "Details coming soon!", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
