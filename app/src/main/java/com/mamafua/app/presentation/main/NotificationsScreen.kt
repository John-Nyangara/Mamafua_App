package com.mamafua.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Notification
import com.mamafua.app.domain.repository.NotificationRepository
import com.mamafua.app.ui.components.EmptyStateView
import com.mamafua.app.ui.components.GradientTopBar
import com.mamafua.app.ui.theme.PremiumTeal
import com.mamafua.app.ui.theme.PremiumTealDark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    val notifications = notificationRepository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                notificationRepository.fetchNotifications()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GradientTopBar(
            title = "Notifications",
            subtitle = "Stay updated on your jobs",
            gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(PremiumTealDark, PremiumTeal)
            ),
            onBackClick = onBackClick
        )

        if (notifications.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.NotificationsNone,
                title = "No Notifications",
                subtitle = "You're all caught up!",
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
                items(notifications) { notification ->
                    NotificationItem(notification = notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = PremiumTealDark,
                modifier = Modifier.padding(top = 4.dp, end = 16.dp)
            )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notification.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
