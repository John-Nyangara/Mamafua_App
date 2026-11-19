package com.mamafua.app.presentation.worker

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.data.remote.CreateAvailabilitySlotRequest
import com.mamafua.app.data.remote.CreateOrUpdateWorkerRequest
import com.mamafua.app.domain.model.AvailabilitySlot
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.domain.model.User
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.ui.components.OsmLocationPickerCard
import com.mamafua.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class WorkerProfileViewModel(
    private val userRepository: UserRepository,
    private val workerRepository: WorkerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _workerProfile = MutableStateFlow<WorkerProfile?>(null)
    val workerProfile: StateFlow<WorkerProfile?> = _workerProfile.asStateFlow()

    private val _availabilitySlots = MutableStateFlow<List<AvailabilitySlot>>(emptyList())
    val availabilitySlots: StateFlow<List<AvailabilitySlot>> = _availabilitySlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = runCatching { userRepository.getProfile() }.getOrNull()
            if (currentUser != null) {
                _user.value = currentUser
                val wpRes = workerRepository.getWorkerById(currentUser.id)
                if (wpRes is Resource.Success) {
                    _workerProfile.value = wpRes.data
                }
            }
            val availRes = workerRepository.getMyAvailability()
            if (availRes is Resource.Success) {
                _availabilitySlots.value = availRes.data.orEmpty()
            }
            _isLoading.value = false
        }
    }

    fun saveWorkerMarketplaceProfile(
        service: String,
        estate: String,
        price: Double,
        bio: String,
        idNumber: String,
        badges: List<String>,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            val current = _workerProfile.value
            val selectedEstateObj = EldoretCatalogDefaults.defaultEstates.find {
                it.name.equals(estate, ignoreCase = true)
            }
            val isCreate = current == null || current.id == 0
            val request = CreateOrUpdateWorkerRequest(
                service = service,
                location = "$estate, Eldoret",
                estate = estate,
                price = price,
                hourlyRate = price,
                bio = bio,
                skills = badges,
                badges = badges,
                idNumber = idNumber.takeIf { it.isNotBlank() },
                isIdVerified = idNumber.length >= 6,
                latitude = latitude ?: selectedEstateObj?.latitude,
                longitude = longitude ?: selectedEstateObj?.longitude
            )
            when (val res = workerRepository.createOrUpdateWorkerProfile(isCreate, request)) {
                is Resource.Success -> {
                    _actionMessage.value = "Mama Fua Marketplace Profile saved!"
                    loadAll()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Failed to save profile: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun addScheduleSlot(dayOfWeek: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            val req = CreateAvailabilitySlotRequest(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                isAvailable = true
            )
            when (val res = workerRepository.addAvailabilitySlot(req)) {
                is Resource.Success -> {
                    _actionMessage.value = "Added $dayOfWeek ($startTime - $endTime) to schedule!"
                    val availRes = workerRepository.getMyAvailability()
                    if (availRes is Resource.Success) {
                        _availabilitySlots.value = availRes.data.orEmpty()
                    }
                }
                is Resource.Error -> {
                    _actionMessage.value = "Could not add slot: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun deleteScheduleSlot(slotId: Int) {
        viewModelScope.launch {
            when (val res = workerRepository.deleteAvailabilitySlot(slotId)) {
                is Resource.Success -> {
                    _availabilitySlots.value = _availabilitySlots.value.filterNot { it.id == slotId }
                    _actionMessage.value = "Schedule slot removed."
                }
                is Resource.Error -> {
                    _actionMessage.value = "Could not remove slot: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun switchToClientMode(onSwitched: () -> Unit) {
        viewModelScope.launch {
            when (val res = userRepository.switchRole("client")) {
                is Resource.Success -> {
                    onSwitched()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Role switch failed: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    viewModel: WorkerProfileViewModel = koinViewModel(),
    onRoleSwitched: () -> Unit = {},
    onLoggedOut: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val workerProfile by viewModel.workerProfile.collectAsState()
    val availabilitySlots by viewModel.availabilitySlots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var selectedEstate by remember(workerProfile) {
        mutableStateOf(workerProfile?.estate ?: "Kapsoya")
    }
    val initialCoords = remember(workerProfile) {
        EldoretCatalogDefaults.resolveCoordinates(
            workerProfile?.latitude,
            workerProfile?.longitude,
            workerProfile?.estate,
            workerProfile?.location
        )
    }
    var pinnedLat by remember(workerProfile) { mutableStateOf(initialCoords.first) }
    var pinnedLng by remember(workerProfile) { mutableStateOf(initialCoords.second) }
    var selectedService by remember(workerProfile) {
        mutableStateOf(workerProfile?.service ?: "Standard Laundry (Fua & Pasi)")
    }
    var priceText by remember(workerProfile) {
        mutableStateOf((workerProfile?.displayRate?.takeIf { it > 0 } ?: 600.0).toInt().toString())
    }
    var idNumber by remember(workerProfile) {
        mutableStateOf(workerProfile?.idNumber ?: "")
    }
    var bio by remember(workerProfile) {
        mutableStateOf(workerProfile?.bio ?: "Experienced Mama Fua in Eldoret — fast, thorough & reliable.")
    }
    var selectedBadges by remember(workerProfile) {
        mutableStateOf(
            workerProfile?.badges?.takeIf { it.isNotEmpty() }
                ?: listOf("Fast Ironer", "Deep Clean Pro")
        )
    }

    var showAddSlotDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumOrangeDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PremiumOrangeSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.name?.take(1) ?: "M").uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = PremiumOrangeDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user?.name ?: "Mama Fua Partner",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (workerProfile?.isIdVerified == true || idNumber.length >= 6) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified ID",
                                            tint = SuccessGreenDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${user?.phone ?: ""} • ${selectedEstate}, Eldoret",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${"%.1f".format(workerProfile?.averageRating ?: 5.0)} (${workerProfile?.completedJobsCount ?: 0} jobs)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Marketplace Profile Editor Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Eldoret Marketplace & Trust Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clients see your estate, badges, and verified ID badge when browsing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Primary Eldoret Estate & Base Pin (5 km Dispatch Radius)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OsmLocationPickerCard(
                                title = "Pin Your Mama Fua Base Location",
                                subtitle = "Used by Auto-Dispatch to match you with clients within 5 km",
                                latitude = pinnedLat,
                                longitude = pinnedLng,
                                selectedEstateName = selectedEstate,
                                addressLabel = "$selectedEstate, Eldoret",
                                estates = EldoretCatalogDefaults.defaultEstates,
                                readOnly = false,
                                radiusMeters = 5000.0,
                                mapHeight = 210.dp,
                                accentColor = PremiumOrangeDark,
                                onLocationSelected = { lat, lng, est, _ ->
                                    pinnedLat = lat
                                    pinnedLng = lng
                                    selectedEstate = est.name
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Specialty Service Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EldoretCatalogDefaults.defaultCategories) { cat ->
                                    FilterChip(
                                        selected = selectedService.equals(cat, ignoreCase = true),
                                        onClick = { selectedService = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Base Rate (KSh)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = idNumber,
                                    onValueChange = { idNumber = it },
                                    label = { Text("National ID No.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Skill Badges (Select your specialties)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EldoretCatalogDefaults.defaultWorkerBadges) { badge ->
                                    val isSelected = selectedBadges.contains(badge)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedBadges = if (isSelected) {
                                                selectedBadges - badge
                                            } else {
                                                selectedBadges + badge
                                            }
                                        },
                                        label = { Text(badge) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Short Bio for Clients") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    viewModel.saveWorkerMarketplaceProfile(
                                        service = selectedService,
                                        estate = selectedEstate,
                                        price = priceText.toDoubleOrNull() ?: 600.0,
                                        bio = bio,
                                        idNumber = idNumber,
                                        badges = selectedBadges,
                                        latitude = pinnedLat,
                                        longitude = pinnedLng
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Marketplace Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Weekly Availability Schedule Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Weekly Availability Calendar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Set days & hours you are available for scheduled bookings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { showAddSlotDialog = true }) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "Add Slot", tint = PremiumOrangeDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (availabilitySlots.isEmpty()) {
                                Text(
                                    text = "No custom slots set (Available Mon–Sat 08:00–17:00 by default).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                availabilitySlots.forEach { slot ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = SuccessGreenDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${slot.dayOfWeek}: ${slot.startTime} - ${slot.endTime}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteScheduleSlot(slot.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRedDark, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Role Switch & Logout Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.switchToClientMode(onRoleSwitched) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Client Mode")
                        }
                        Button(
                            onClick = { viewModel.logout(onLoggedOut) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRedDark),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out")
                        }
                    }
                }
            }
        }
    }

    if (showAddSlotDialog) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var day by remember { mutableStateOf("Monday") }
        var start by remember { mutableStateOf("08:00") }
        var end by remember { mutableStateOf("17:00") }

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text("Add Availability Slot", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(days) { d ->
                            FilterChip(
                                selected = day == d,
                                onClick = { day = d },
                                label = { Text(d.take(3)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addScheduleSlot(day, start, end)
                        showAddSlotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumOrangeDark)
                ) {
                    Text("Add Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
