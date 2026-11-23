package com.mamafua.app.presentation.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.RadarOffer
import com.mamafua.app.presentation.main.NotificationsViewModel
import com.mamafua.app.ui.components.GradientTopBar
import com.mamafua.app.ui.components.OsmLocationPickerCard
import com.mamafua.app.ui.components.OsmMapMarker
import com.mamafua.app.ui.components.PulsingDot
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    viewModel: WorkerHomeViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToActiveJob: (Int) -> Unit = {}
) {
    val feedState by viewModel.feedState.collectAsState()
    val radarOffers by viewModel.radarOffers.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val selectedEstate by viewModel.selectedEstate.collectAsState()
    val workerLat by viewModel.workerLatitude.collectAsState()
    val workerLng by viewModel.workerLongitude.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Mama Fua Radar",
            subtitle = "Live Eldoret Jobs & Instant Direct Offers",
            gradient = Brush.verticalGradient(listOf(PremiumOrangeDark, PremiumOrange)),
            actions = {
                IconButton(onClick = { viewModel.refreshAll() }) {
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

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
            // 0. Hero Section (Worker Dashboard)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(140.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?auto=format&fit=crop&w=1000&q=80",
                            contentDescription = "Worker Hero",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, PremiumOrangeDark.copy(alpha = 0.9f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Ready to hustle?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Go online to receive instant jobs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Header + Availability Online/Offline Toggle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Online / Offline Switch Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isOnline) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) SuccessGreenDark else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isOnline) "You are ONLINE" else "You are OFFLINE",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) SuccessGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isOnline) "Receiving instant Eldoret job pings nearby" else "Switch online to receive instant job dispatches",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { viewModel.toggleOnlineStatus(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreenDark
                                )
                            )
                        }
                    }
                }
            }

            // Active In-Progress Gig Hero Card
            if (activeJob != null) {
                val gig = activeJob!!
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .border(1.5.dp, PremiumOrangeDark, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PulsingDot(color = PremiumOrangeDark, size = 10.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (gig.status) {
                                            JobStatus.ARRIVED -> "YOU ARRIVED — ENTER CLIENT PIN"
                                            JobStatus.IN_PROGRESS -> "CLEANING IN PROGRESS"
                                            JobStatus.COMPLETION_REQUESTED -> "AWAITING CLIENT CONFIRMATION"
                                            else -> "ACTIVE GIG #${gig.id}"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumOrangeDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PremiumOrangeSurface
                                ) {
                                    Text(
                                        text = "Net KES ${(gig.budget * 0.9).toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumOrangeDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gig.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${gig.estate ?: gig.location} • Status: ${gig.status.displayLabel.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val (gigLat, gigLng) = EldoretCatalogDefaults.resolveCoordinates(
                                gig.latitude, gig.longitude, gig.estate, gig.location
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigateToActiveJob(gig.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.5f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (gig.status) {
                                            JobStatus.ASSIGNED, JobStatus.ACCEPTED -> "View Details & PIN"
                                            JobStatus.ARRIVED -> "Enter Handshake PIN"
                                            else -> "Active Gig Details"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        val uri = Uri.parse("google.navigation:q=$gigLat,$gigLng")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            val geoUri = Uri.parse("geo:$gigLat,$gigLng?q=$gigLat,$gigLng(${Uri.encode(gig.estate ?: gig.location ?: "Site")})")
                                            runCatching<Unit> { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = "Navigate Site", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Incoming Radar Requests Section (High-Contrast Cards)
            if (radarOffers.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Radar,
                                contentDescription = null,
                                tint = PremiumOrangeDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Incoming Instant Requests (${radarOffers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PremiumOrangeDark
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                items(radarOffers) { offer ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        RadarOfferCard(
                            offer = offer,
                            onAccept = {
                                viewModel.respondToRadar(
                                    offerId = offer.offerId,
                                    jobId = offer.jobId,
                                    action = "accept",
                                    onAccepted = onNavigateToActiveJob
                                )
                            },
                            onDecline = {
                                viewModel.respondToRadar(
                                    offerId = offer.offerId,
                                    jobId = offer.jobId,
                                    action = "decline"
                                )
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Eldoret Live Job Radar OpenStreetMap + Estate Filter Bar
            item {
                val openJobsList = (feedState as? WorkerFeedState.Success)?.jobs.orEmpty()
                val radarMarkers = remember(radarOffers, openJobsList) {
                    val offerPins = radarOffers.mapIndexed { idx, offer ->
                        val (lat, lng) = EldoretCatalogDefaults.resolveCoordinates(
                            offer.latitude,
                            offer.longitude,
                            offer.estate,
                            offer.location
                        )
                        OsmMapMarker(
                            id = offer.jobId,
                            latitude = lat + (idx * 0.0012),
                            longitude = lng + (idx * 0.0012),
                            title = offer.title,
                            subtitle = "${offer.estate ?: offer.location ?: "Eldoret"} • Net KSh ${offer.estimatedPayout.toInt()}",
                            badgeText = "KSh ${offer.estimatedPayout.toInt()}",
                            colorHex = "#F77F00",
                            isPulsing = true
                        )
                    }
                    val openJobPins = openJobsList.mapIndexed { idx, job ->
                        val (lat, lng) = EldoretCatalogDefaults.resolveCoordinates(
                            job.latitude,
                            job.longitude,
                            job.estate,
                            job.location
                        )
                        val offsetLat = lat + ((idx % 4) - 1.5) * 0.0016
                        val offsetLng = lng + ((idx / 4) - 1) * 0.0016
                        OsmMapMarker(
                            id = job.id,
                            latitude = offsetLat,
                            longitude = offsetLng,
                            title = job.title,
                            subtitle = "${job.estate ?: job.location} • KSh ${job.budget.toInt()}",
                            badgeText = "KSh ${job.budget.toInt()}",
                            colorHex = "#0077B6",
                            isPulsing = false
                        )
                    }
                    offerPins + openJobPins
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    OsmLocationPickerCard(
                        title = "Live Eldoret Job Radar Map (OSM)",
                        subtitle = "Tap My GPS or move your pin • Orange = Instant Radar, Blue = Open Jobs (5 km circle)",
                        latitude = workerLat,
                        longitude = workerLng,
                        selectedEstateName = selectedEstate,
                        addressLabel = "${radarOffers.size} instant pings • ${openJobsList.size} open jobs on map",
                        estates = EldoretCatalogDefaults.defaultEstates,
                        readOnly = false,
                        radiusMeters = 5000.0,
                        markers = radarMarkers,
                        mapHeight = 230.dp,
                        accentColor = PremiumOrangeDark,
                        onLocationSelected = { lat, lng, est, _ ->
                            viewModel.updateWorkerRadarCenter(lat, lng, est.name)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Filter by Eldoret Estate",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedEstate == null,
                            onClick = { viewModel.selectEstateFilter(null) },
                            label = { Text("All Eldoret") }
                        )
                    }
                    items(EldoretCatalogDefaults.defaultEstates) { est ->
                        FilterChip(
                            selected = selectedEstate.equals(est.name, ignoreCase = true),
                            onClick = {
                                viewModel.selectEstateFilter(
                                    if (selectedEstate.equals(est.name, ignoreCase = true)) null else est.name
                                )
                            },
                            label = { Text(est.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Open Marketplace Jobs Section
            item {
                Text(
                    text = "Open Marketplace Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            when (val state = feedState) {
                is WorkerFeedState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PremiumOrangeDark)
                        }
                    }
                }
                is WorkerFeedState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadOpenJobs() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is WorkerFeedState.Success -> {
                    if (state.jobs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No open jobs in ${selectedEstate ?: "Eldoret"} right now.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.jobs) { job ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                WorkerJobItemCard(
                                    job = job,
                                    onApply = { viewModel.applyForJob(job.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun RadarOfferCard(
    offer: RadarOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PremiumOrangeDark, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumOrangeSurface.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PremiumOrangeDark,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (offer.dispatchType == "direct_request") "DIRECT CLIENT REQUEST" else "INSTANT RADAR PING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (offer.secondsRemaining > 0) {
                    Text(
                        text = "${offer.secondsRemaining / 60}m ${offer.secondsRemaining % 60}s left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRedDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PremiumOrangeDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = offer.estate ?: offer.location ?: "Eldoret",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                offer.distanceKm?.let { km ->
                    Text(
                        text = " • ${"%.1f".format(km)} km away",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Net Payout (90%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "KSh ${offer.estimatedPayout.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreenDark
                    )
                }
                if (offer.workerBringsDetergents) {
                    Surface(
                        color = PremiumTealSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Bring Detergents (+KSh 250)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PremiumTealDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Decline", color = ErrorRedDark)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept & Lock Job", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WorkerJobItemCard(
    job: Job,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PremiumOrangeDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.estate ?: job.location,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = PremiumOrangeSurface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "KSh ${job.budget.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumOrangeDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val escrowLabel = when ((job.paymentStatus ?: "").lowercase()) {
                    "escrow_held", "paid" -> "Escrow Funded"
                    "partial_escrow" -> "50% Escrow Held"
                    else -> "Direct Booking"
                }
                Text(
                    text = escrowLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("Apply Now", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
