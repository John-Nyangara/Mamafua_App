package com.mamafua.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Notification
import com.mamafua.app.domain.repository.NotificationRepository
import com.mamafua.app.ui.components.EmptyStateView
import com.mamafua.app.ui.components.GradientTopBar
import com.mamafua.app.ui.theme.PremiumOrangeDark
import com.mamafua.app.ui.theme.PremiumTeal
import com.mamafua.app.ui.theme.PremiumTealDark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.koin.androidx.compose.koinViewModel

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    val notifications = notificationRepository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            notificationRepository.fetchNotifications()
        }
    }

    fun markRead(notificationId: Int) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun extractJobId(dataJson: String?): Int? {
        if (dataJson.isNullOrBlank()) return null
        return runCatching {
            val obj = Json.parseToJsonElement(dataJson) as? JsonObject ?: return null
            obj["jobId"]?.jsonPrimitive?.intOrNull
                ?: obj["job_id"]?.jsonPrimitive?.intOrNull
        }.getOrNull()
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onOpenJob: ((Int) -> Unit)? = null
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GradientTopBar(
            title = "Notifications",
            subtitle = if (unreadCount > 0) "$unreadCount unread updates" else "Stay updated on your bookings",
            gradient = Brush.verticalGradient(colors = listOf(PremiumTealDark, PremiumTeal)),
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        )

        if (notifications.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.NotificationsNone,
                title = "No Notifications",
                subtitle = "Job radar alerts, Start PINs, and scope updates will appear here.",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    val linkedJobId = viewModel.extractJobId(notification.data)
                    NotificationItem(
                        notification = notification,
                        hasLinkedJob = linkedJobId != null && onOpenJob != null,
                        onClick = {
                            if (!notification.isRead) {
                                viewModel.markRead(notification.id)
                            }
                            if (linkedJobId != null && onOpenJob != null) {
                                onOpenJob(linkedJobId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    hasLinkedJob: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (notification.isRead) Color.Gray else PremiumTealDark,
                    modifier = Modifier.padding(end = 14.dp)
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PremiumOrangeDark, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notification.createdAt != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = notification.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            if (hasLinkedJob) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Job",
                    tint = PremiumTealDark
                )
            }
        }
    }
}
