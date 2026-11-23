package com.mamafua.app.presentation.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun PostJobScreen(
    viewModel: PostJobViewModel = koinViewModel(),
    initialCategory: String? = null,
    preselectedWorkerId: Int? = null,
    preselectedWorkerName: String? = null,
    onJobPosted: (jobId: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(initialCategory, preselectedWorkerId, preselectedWorkerName) {
        viewModel.applyPreselection(initialCategory, preselectedWorkerId, preselectedWorkerName)
    }

    val workerMarkers = remember(uiState.availableOnlineWorkers) {
        uiState.availableOnlineWorkers.mapIndexed { idx, w ->
            val (wLat, wLng) = EldoretCatalogDefaults.resolveCoordinates(
                w.latitude,
                w.longitude,
                w.estate,
                w.location
            )
            val jitterLat = wLat + ((idx % 3) - 1) * 0.0018
            val jitterLng = wLng + ((idx / 3) - 1) * 0.0018
            OsmMapMarker(
                id = w.resolvedUserId,
                latitude = jitterLat,
                longitude = jitterLng,
                title = w.displayName,
                subtitle = "${w.estate ?: "Eldoret"} • ★${"%.1f".format(w.averageRating ?: 5.0)}",
                badgeText = w.displayName.split(" ").firstOrNull() ?: "Mama Fua",
                colorHex = "#2DC653",
                isPulsing = false
            )
        }
    }

    val hasSelectedItems = uiState.itemQuantities.values.any { it > 0 } ||
            uiState.addonQuantities.values.any { it > 0 } ||
            !uiState.isCustomizerMode

    val effectiveTotal = if (uiState.isCustomizerMode) {
        uiState.estimate.totalEstimatedPrice.toInt()
    } else {
        viewModel.budget.toIntOrNull() ?: 0
    }

    Scaffold(
        topBar = {
            Column {
                GradientTopBar(
                    title = "Book a Mama Fua",
                    subtitle = "Eldoret Customized Service & Live Escrow"
                )
                // 3-Step Wizard Navigation Stepper
                BookingWizardStepper(
                    currentStep = currentStep,
                    onStepClick = { step ->
                        if (step < currentStep || hasSelectedItems) {
                            currentStep = step
                        }
                    }
                )
            }
        },
        bottomBar = {
            // Sticky Bottom Summary & Navigation Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (effectiveTotal > 0) "KES $effectiveTotal" else "Select Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumTealDark
                        )
                        if (effectiveTotal > 0) {
                            Text(
                                text = "50% Deposit: KES ${(effectiveTotal * 0.5).toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumOrangeDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Live price estimator",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        }

                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> currentStep = 2
                                    2 -> currentStep = 3
                                    3 -> viewModel.submitBooking(onJobPosted)
                                }
                            },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStep == 3) PremiumOrangeDark else PremiumTealDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Booking...")
                            } else {
                                when (currentStep) {
                                    1 -> {
                                        Text("Next: Location", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    2 -> {
                                        Text("Next: Matching", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    3 -> {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Confirm & Book", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switcher: Service Customizer vs Manual Job (Only on Step 1)
            if (currentStep == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.isCustomizerMode,
                        onClick = { viewModel.setBookingMode(true) },
                        label = { Text("Service Customizer") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !uiState.isCustomizerMode,
                        onClick = { viewModel.setBookingMode(false) },
                        label = { Text("Custom Budget") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── STEP 1: SERVICES & MICRO-TASKS ──
            if (currentStep == 1) {
                if (uiState.isCustomizerMode) {
                    PremiumCard {
                        Text(
                            text = "1. Select Laundry & Cleaning Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.catalog.categories) { category ->
                                FilterChip(
                                    selected = uiState.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = { Text(category) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PremiumTealSurface)
                        ) {
                            AsyncImage(
                                model = EldoretCatalogDefaults.getCategoryImage(uiState.selectedCategory),
                                contentDescription = uiState.selectedCategory,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val servicesInCategory = uiState.catalog.services.filter {
                            it.category.equals(uiState.selectedCategory, ignoreCase = true)
                        }.ifEmpty { uiState.catalog.services }

                        servicesInCategory.forEachIndexed { idx, svc ->
                            val qty = uiState.itemQuantities[svc.code] ?: 0
                            var showChecklist by remember { mutableStateOf(false) }

                            if (idx > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = svc.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "KES ${svc.basePrice.toInt()} ${svc.unitLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PremiumOrangeDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (svc.checklist.isNotEmpty()) {
                                        Text(
                                            text = if (showChecklist) "Hide checklist ▲" else "Includes ${svc.checklist.size} steps ▼",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumTealDark,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .clickable { showChecklist = !showChecklist }
                                        )
                                    }
                                }

                                QuantityStepper(
                                    quantity = qty,
                                    onDecrement = { viewModel.updateServiceQuantity(svc.code, -1) },
                                    onIncrement = { viewModel.updateServiceQuantity(svc.code, +1) }
                                )
                            }

                            if (showChecklist && svc.checklist.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                    ) {
                                        svc.checklist.forEach { step ->
                                            Text(
                                                text = "✓ $step",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    // Micro-Task Add-Ons & Detergent Provision Toggle
                    PremiumCard {
                        Text(
                            text = "Micro-Task Add-Ons & Detergents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.catalog.addons.forEachIndexed { idx, addon ->
                            val qty = uiState.addonQuantities[addon.code] ?: 0
                            if (idx > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(addon.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "+KES ${addon.price.toInt()} (${addon.unitLabel})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PremiumTealDark
                                    )
                                }
                                QuantityStepper(
                                    quantity = qty,
                                    onDecrement = { viewModel.updateAddonQuantity(addon.code, -1) },
                                    onIncrement = { viewModel.updateAddonQuantity(addon.code, +1) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = uiState.catalog.detergentToggle.workerBringsLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.workerBringsDetergents)
                                        "+KES ${uiState.catalog.detergentToggle.workerBringsFee.toInt()} added"
                                    else
                                        uiState.catalog.detergentToggle.clientProvidesLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.workerBringsDetergents,
                                onCheckedChange = { viewModel.toggleDetergents(it) }
                            )
                        }
                    }

                    // Live Price Estimator Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PremiumTealSurface.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PremiumTealDark.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live Price Estimate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumBlue
                                )
                                if (uiState.isEstimating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            EstimateRow("Services Subtotal", "KES ${uiState.estimate.servicesSubtotal.toInt()}")
                            EstimateRow("Add-Ons Subtotal", "KES ${uiState.estimate.addonsSubtotal.toInt()}")
                            EstimateRow("Detergents Fee", "KES ${uiState.estimate.detergentFee.toInt()}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Estimated Price", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "KES ${uiState.estimate.totalEstimatedPrice.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PremiumTealDark
                                )
                            }
                        }
                    }
                } else {
                    // Manual Job Fields
                    PremiumCard {
                        Text(
                            text = "Custom Job Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedTextField(
                            value = viewModel.title,
                            onValueChange = { viewModel.title = it },
                            label = "Job Title",
                            leadingIcon = Icons.Default.Work
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedTextField(
                            value = viewModel.description,
                            onValueChange = { viewModel.description = it },
                            label = "Task Description",
                            leadingIcon = Icons.Default.Description,
                            singleLine = false,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedTextField(
                            value = viewModel.budget,
                            onValueChange = { viewModel.budget = it },
                            label = "Budget (KES)",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // ── STEP 2: GATE PIN & SCHEDULE ──
            if (currentStep == 2) {
                OsmLocationPickerCard(
                    title = "Pin Your Exact Gate on OpenStreetMap",
                    subtitle = "Tap your house/hostel on the map or use My GPS • Green pins show online Mama Fuas nearby",
                    latitude = uiState.pinnedLatitude,
                    longitude = uiState.pinnedLongitude,
                    selectedEstateName = uiState.selectedEstate.name,
                    addressLabel = viewModel.location,
                    estates = uiState.catalog.estates,
                    readOnly = false,
                    radiusMeters = 3000.0,
                    markers = workerMarkers,
                    accentColor = PremiumTealDark,
                    onLocationSelected = { lat, lng, nearestEst, roadAddr ->
                        viewModel.updatePinnedLocation(lat, lng, nearestEst, roadAddr)
                    },
                    onMarkerClicked = { workerId ->
                        uiState.availableOnlineWorkers.find { it.resolvedUserId == workerId }?.let { w ->
                            viewModel.selectPreferredWorker(w)
                        }
                    }
                )

                PremiumCard {
                    Text(
                        text = "Apartment / Gate / Landmark Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Auto-filled from OpenStreetMap pin (${uiState.selectedEstate.name} • ${"%.4f".format(uiState.pinnedLatitude)}, ${"%.4f".format(uiState.pinnedLongitude)}). Add apartment/floor details if needed:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedTextField(
                        value = viewModel.location,
                        onValueChange = { viewModel.location = it },
                        label = "Apartment / Landmark (e.g. Near Shopping Center, Apt B4)",
                        leadingIcon = Icons.Default.LocationOn
                    )
                }

                PremiumCard {
                    Text(
                        text = "Schedule Dispatch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = uiState.isInstant,
                            onClick = { viewModel.setInstantBooking(true) },
                            label = { Text("Book Now (Instant)") }
                        )
                        FilterChip(
                            selected = !uiState.isInstant,
                            onClick = { viewModel.setInstantBooking(false) },
                            label = { Text("Schedule Later") }
                        )
                    }
                    if (!uiState.isInstant) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.scheduledDate,
                                onValueChange = { viewModel.updateSchedule(it, uiState.scheduledTime) },
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.scheduledTime,
                                onValueChange = { viewModel.updateSchedule(uiState.scheduledDate, it) },
                                label = { Text("Time (HH:MM)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── STEP 3: MATCHING ENGINE & FINAL REVIEW ──
            if (currentStep == 3) {
                PremiumCard {
                    Text(
                        text = "Choose Matching Engine Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MatchingModeOption(
                        title = "Auto-Dispatch (Uber Mode)",
                        subtitle = "Pings top 3 online Mama Fuas within 5 km of ${uiState.selectedEstate.name}",
                        selected = uiState.matchingMode == "auto_dispatch",
                        onClick = { viewModel.selectMatchingMode("auto_dispatch") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MatchingModeOption(
                        title = "Direct Booking (15-min Lock)",
                        subtitle = uiState.preferredWorkerName?.let { "Selected: $it" }
                            ?: "Pick a specific Mama Fua below",
                        selected = uiState.matchingMode == "direct_booking",
                        onClick = { viewModel.selectMatchingMode("direct_booking") }
                    )
                    if (uiState.matchingMode == "direct_booking" && uiState.availableOnlineWorkers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.availableOnlineWorkers) { w ->
                                FilterChip(
                                    selected = uiState.preferredWorkerId == w.resolvedUserId,
                                    onClick = { viewModel.selectPreferredWorker(w) },
                                    label = { Text("${w.displayName} (★${String.format("%.1f", w.averageRating ?: 5.0)})") }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MatchingModeOption(
                        title = "Open Marketplace Board",
                        subtitle = "Post to job board and review Mama Fua applicants yourself",
                        selected = uiState.matchingMode == "marketplace",
                        onClick = { viewModel.selectMatchingMode("marketplace") }
                    )
                }

                // Final Order Summary
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PremiumTealDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Booking Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        EstimateRow("Estate / Location", "${uiState.selectedEstate.name}, Eldoret")
                        if (viewModel.location.isNotBlank()) {
                            EstimateRow("Landmark / Gate", viewModel.location)
                        }
                        EstimateRow("Timing", if (uiState.isInstant) "Instant Booking" else "${uiState.scheduledDate} at ${uiState.scheduledTime}")
                        EstimateRow("Detergents", if (uiState.workerBringsDetergents) "Worker brings (+KES 250)" else "Client provides")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Escrow Payable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "KES $effectiveTotal",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = PremiumTealDark
                            )
                        }
                    }
                }
            }

            // Error Alerts
            uiState.errorMessage?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRedDark)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMsg, color = ErrorRedDark, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BookingWizardStepper(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf("1. Tasks", "2. Location", "3. Matching")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isActive = currentStep == stepNumber
            val isDone = currentStep > stepNumber

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStepClick(stepNumber) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> SuccessGreenDark
                                isActive -> PremiumTealDark
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.substringAfter(". "),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) PremiumTealDark else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onDecrement,
            enabled = quantity > 0,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (quantity > 0) PremiumTealSurface else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
        }
        Text(
            text = "$quantity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(PremiumTealDark)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EstimateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MatchingModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) PremiumTealSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PremiumTealDark else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
