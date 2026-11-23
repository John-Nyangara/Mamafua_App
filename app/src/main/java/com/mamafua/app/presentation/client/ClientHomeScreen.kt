package com.mamafua.app.presentation.client

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.presentation.main.NotificationsViewModel
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientHomeScreen(
    viewModel: ClientHomeViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCustomizer: (category: String?, workerId: Int?, workerName: String?) -> Unit = { _, _, _ -> },
    onNavigateToJobDetail: (jobId: Int) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    var favoriteToSchedule by remember { mutableStateOf<WorkerProfile?>(null) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Mama Fua Eldoret",
            subtitle = "Book trusted laundry & house cleaning",
            actions = {
                IconButton(onClick = { viewModel.loadDashboard() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onNavigateToNotifications) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(containerColor = ErrorRed) {
                                    Text(unreadCount.toString(), color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )
=======
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Nearby Workers", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onNavigateToProfile) {
                Text("Profile")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
>>>>>>> main

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 0. Hero Section (Welcome & Visuals)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=1000&q=80",
                            contentDescription = "Hero Background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, PremiumBlue.copy(alpha = 0.85f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Welcome back, Client",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Let's get your home shining today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // 1. Active Booking Hero Banner (Pinned when job is active)
            if (uiState.activeJob != null) {
                val active = uiState.activeJob!!
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, PremiumBlueMid, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PulsingDot(
                                        color = if (active.status == JobStatus.ARRIVED) PremiumOrangeDark else SuccessGreenDark,
                                        size = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (active.status) {
                                            JobStatus.ARRIVED -> "MAMA FUA ARRIVED AT YOUR GATE!"
                                            JobStatus.IN_PROGRESS -> "CLEANING IN PROGRESS"
                                            JobStatus.COMPLETION_REQUESTED -> "CLEANING COMPLETE — CONFIRM"
                                            JobStatus.ASSIGNED, JobStatus.ACCEPTED -> "MAMA FUA ON THE WAY"
                                            else -> "ACTIVE BOOKING #${active.id}"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active.status == JobStatus.ARRIVED) PremiumOrangeDark else PremiumBlueMid
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PremiumBlue.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "KES ${active.budget.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumBlueMid,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = active.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${active.estate ?: active.location} • ${active.status.displayLabel.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!active.startPin.isNullOrBlank() && active.status in listOf(JobStatus.ASSIGNED, JobStatus.ACCEPTED, JobStatus.ARRIVED)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = PremiumBlue.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "4-Digit Handshake PIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PremiumBlue
                                            )
                                            Text(
                                                text = "Give to Mama Fua at the gate",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = PremiumBlue,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = active.startPin.orEmpty(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigateToJobDetail(active.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active.status == JobStatus.COMPLETION_REQUESTED) SuccessGreenDark else PremiumBlueMid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text(
                                    text = if (active.status == JobStatus.COMPLETION_REQUESTED) "Confirm & Release Escrow" else "View Booking Status",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 1. Quick Action Banner: Instant Book + 2-Tap Rebook Last Mama Fua
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PremiumBlueLight, PremiumBlueMid)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Instant Eldoret Dispatch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Customize baskets, house rooms & add-ons with upfront M-Pesa escrow protection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToCustomizer(null, null, null) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PremiumOrange,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Book Now", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.rebookLastMamaFua { jobId ->
                                            onNavigateToJobDetail(jobId)
                                        }
                                    },
                                    enabled = !uiState.isRebooking,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    if (uiState.isRebooking) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rebook Last")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. My Favorite Mama Fuas Carousel (2-Tap Rebooking)
            if (uiState.favorites.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Favorite Mama Fuas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "2-Tap Rebook",
                                style = MaterialTheme.typography.labelMedium,
                                color = PremiumBlueMid,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.favorites, key = { it.resolvedUserId }) { fav ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    modifier = Modifier.width(230.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AvatarCircle(name = fav.displayName, imageUrl = fav.profileImageUrl, size = 38.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = fav.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = PremiumOrange,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = " ${String.format("%.1f", fav.averageRating ?: 5.0)}",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        PulsingDot(
                                                            color = if (fav.isOnline) StatusAvailable else StatusOffline,
                                                            size = 6.dp
                                                        )
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.toggleFavorite(fav) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Favorite,
                                                    contentDescription = "Unfavorite",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { favoriteToSchedule = fav },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlueMid),
                                            modifier = Modifier.fillMaxWidth().height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Rebook Now", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Eldoret Service Categories
            item {
                Column {
                    Text(
                        text = "Service Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.catalog.categories) { category ->
                            val imageUrl = EldoretCatalogDefaults.getCategoryImage(category)
                            ElevatedCard(
                                onClick = { onNavigateToCustomizer(category, null, null) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.width(175.dp)
                            ) {
                                Column(modifier = Modifier.padding(0.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(PremiumBlue.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = category,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { onNavigateToCustomizer(category, null, null) },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PremiumBlueLight,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Book Now",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Filter Bar: Online Toggle + Eldoret Estates + Badges
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browse Nearby Mama Fuas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = uiState.onlineOnly,
                            onClick = { viewModel.toggleOnlineOnly(!uiState.onlineOnly) },
                            label = { Text("Online Now") },
                            leadingIcon = {
                                PulsingDot(
                                    color = if (uiState.onlineOnly) StatusAvailable else StatusOffline,
                                    size = 7.dp
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedEstate == null,
                                onClick = { viewModel.selectEstateFilter(null) },
                                label = { Text("All Eldoret") }
                            )
                        }
                        items(uiState.catalog.estates) { estate ->
                            FilterChip(
                                selected = uiState.selectedEstate == estate.name,
                                onClick = {
                                    viewModel.selectEstateFilter(
                                        if (uiState.selectedEstate == estate.name) null else estate.name
                                    )
                                },
                                label = { Text(estate.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EldoretCatalogDefaults.defaultWorkerBadges) { badge ->
                            FilterChip(
                                selected = uiState.selectedBadge == badge,
                                onClick = {
                                    viewModel.selectBadgeFilter(
                                        if (uiState.selectedBadge == badge) null else badge
                                    )
                                },
                                label = { Text("$badge") }
                            )
                        }
                    }
                }
            }

            // 5. Workers List
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (uiState.workers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No Mama Fuas Match Filters",
                        subtitle = "Try clearing the estate or badge filter, or use Instant Auto-Dispatch!"
                    )
                }
            } else {
                items(uiState.workers, key = { "${it.resolvedWorkerId}_${it.resolvedUserId}" }) { worker ->
                    val isFav = uiState.favorites.any {
                        it.resolvedUserId == worker.resolvedUserId || it.resolvedWorkerId == worker.resolvedWorkerId
                    }
                    PremiumCard(onClick = { viewModel.openWorkerDetails(worker) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AvatarCircle(name = worker.displayName, size = 50.dp)
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
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "ID Verified",
                                                tint = PremiumTealDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = listOfNotNull(
                                            worker.estate ?: worker.location,
                                            worker.service
                                        ).joinToString(" • ").ifBlank { "Eldoret Home Services" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (worker.displayRate > 0) {
                                        Text(
                                            text = "From KES ${worker.displayRate.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PremiumOrangeDark
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = PremiumOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", worker.averageRating ?: 0.0),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(worker) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (worker.completedJobsCount > 0) {
                                    Text(
                                        text = "${worker.completedJobsCount} jobs done",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (worker.badges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                worker.badges.forEach { badge ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PremiumOrangeSurface
                                    ) {
                                        Text(
                                            text = "$badge",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumOrangeDark,
                                            fontWeight = FontWeight.SemiBold,
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(
                                    color = if (worker.isOnline) StatusAvailable else StatusOffline,
                                    size = 8.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusChip(
                                    text = (worker.availabilityStatus ?: "offline").uppercase(),
                                    color = if (worker.isOnline) StatusAvailable else StatusOffline
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.openWorkerDetails(worker) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Profile")
                                }
                                Button(
                                    onClick = {
                                        onNavigateToCustomizer(
                                            null,
                                            worker.resolvedUserId,
                                            worker.displayName
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark)
                                ) {
                                    Text("Book Direct")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 2-Tap Favorite Rebooking Dialog (Instant vs Scheduled)
    favoriteToSchedule?.let { fav ->
        var isInstant by remember { mutableStateOf(true) }
        var scheduledDate by remember { mutableStateOf("2026-11-02") }
        var scheduledTime by remember { mutableStateOf("09:00") }

        AlertDialog(
            onDismissRequest = { favoriteToSchedule = null },
            title = { Text("Rebook ${fav.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Send a 15-minute Direct Booking Offer to ${fav.displayName}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isInstant,
                            onClick = { isInstant = true },
                            label = { Text("Instant Now") }
                        )
                        FilterChip(
                            selected = !isInstant,
                            onClick = { isInstant = false },
                            label = { Text("Schedule") }
                        )
                    }
                    if (!isInstant) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = scheduledDate,
                            onValueChange = { scheduledDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scheduledTime,
                            onValueChange = { scheduledTime = it },
                            label = { Text("Time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isoSchedule = if (isInstant) null else "${scheduledDate.trim()}T${scheduledTime.trim()}:00.000Z"
                        favoriteToSchedule = null
                        viewModel.rebookWorker(
                            workerId = fav.resolvedUserId,
                            isInstant = isInstant,
                            scheduledAt = isoSchedule
                        ) { jobId ->
                            onNavigateToJobDetail(jobId)
                        }
                    }
                ) {
                    Text("Send Direct Offer")
                }
            },
            dismissButton = {
                TextButton(onClick = { favoriteToSchedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Worker Full Profile, Reviews, Availability & Trust Modal
    uiState.selectedWorkerDetails?.let { details ->
        var showReportPrompt by remember { mutableStateOf(false) }
        var reportReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissWorkerDetails() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(details.worker.displayName, fontWeight = FontWeight.Bold)
                    if (details.worker.isIdVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = PremiumTealDark)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${details.worker.estate ?: details.worker.location ?: "Eldoret"} • ★ ${String.format("%.1f", details.worker.averageRating ?: 0.0)} (${details.reviewsSummary.totalReviews} reviews)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumTealDark
                    )
                    details.worker.bio?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (details.worker.badges.isNotEmpty()) {
                        Text(
                            text = "Badges: ${details.worker.badges.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PremiumOrangeDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (details.worker.skills.isNotEmpty()) {
                        Text(
                            text = "Skills: ${details.worker.skills.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (details.availability.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Weekly Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        details.availability.forEach { slot ->
                            Text(
                                text = "• ${slot.dayOfWeek}: ${slot.startTime} – ${slot.endTime}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (details.reviewsSummary.reviews.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Recent Client Reviews", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        details.reviewsSummary.reviews.take(4).forEach { rev ->
                            Text(
                                text = "${"★".repeat(rev.rating.coerceIn(1, 5))} ${rev.comment ?: "Great service!"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (showReportPrompt) {
                        HorizontalDivider()
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text("Reason for reporting") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    if (reportReason.isNotBlank()) {
                                        viewModel.reportWorker(details.worker.resolvedUserId, reportReason)
                                    }
                                }
                            ) {
                                Text("Submit Report", color = ErrorRedDark)
                            }
                            TextButton(onClick = { viewModel.blockWorker(details.worker.resolvedUserId) }) {
                                Text("Block User", color = ErrorRedDark)
                            }
                        }
                    } else {
                        TextButton(onClick = { showReportPrompt = true }) {
                            Text("Report or Block User", color = ErrorRedDark, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = details.worker
                        viewModel.dismissWorkerDetails()
                        onNavigateToCustomizer(null, w.resolvedUserId, w.displayName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark)
                ) {
                    Text("Book ${details.worker.displayName}")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWorkerDetails() }) {
                    Text("Close")
                }
            }
        )
    }
}
