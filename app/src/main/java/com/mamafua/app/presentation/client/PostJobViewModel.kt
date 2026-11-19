package com.mamafua.app.presentation.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.data.remote.CreateJobRequest
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.repository.CatalogRepository
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostJobUiState(
    val isCustomizerMode: Boolean = true,
    val catalog: CatalogData = EldoretCatalogDefaults.defaultCatalog(),
    val selectedCategory: String = EldoretCatalogDefaults.defaultCategories.first(),
    val itemQuantities: Map<String, Int> = emptyMap(),
    val addonQuantities: Map<String, Int> = emptyMap(),
    val workerBringsDetergents: Boolean = true,
    val estimate: PriceEstimate = PriceEstimate(),
    val isEstimating: Boolean = false,
    val selectedEstate: EldoretEstate = EldoretCatalogDefaults.defaultEstates.first(),
    val pinnedLatitude: Double = EldoretCatalogDefaults.defaultEstates.first().latitude,
    val pinnedLongitude: Double = EldoretCatalogDefaults.defaultEstates.first().longitude,
    val isInstant: Boolean = true,
    val scheduledDate: String = "2026-11-02",
    val scheduledTime: String = "09:00",
    val matchingMode: String = "auto_dispatch", // "auto_dispatch", "direct_booking", "marketplace"
    val preferredWorkerId: Int? = null,
    val preferredWorkerName: String? = null,
    val availableOnlineWorkers: List<WorkerProfile> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class PostJobViewModel(
    private val jobRepository: JobRepository,
    private val catalogRepository: CatalogRepository,
    private val workerRepository: WorkerRepository
) : ViewModel() {

    var location by mutableStateOf("")
    // Manual fallback fields
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var budget by mutableStateOf("")

    private val _uiState = MutableStateFlow(PostJobUiState())
    val uiState = _uiState.asStateFlow()

    private var estimateJob: CoroutineJob? = null

    init {
        loadCatalogAndWorkers()
    }

    private fun loadCatalogAndWorkers() {
        viewModelScope.launch {
            val catalog = catalogRepository.getCatalog()
            val firstCategory = catalog.categories.firstOrNull() ?: EldoretCatalogDefaults.defaultCategories.first()
            val firstEstate = catalog.estates.firstOrNull() ?: EldoretCatalogDefaults.defaultEstates.first()
            _uiState.value = _uiState.value.copy(
                catalog = catalog,
                selectedCategory = firstCategory,
                selectedEstate = firstEstate,
                pinnedLatitude = firstEstate.latitude,
                pinnedLongitude = firstEstate.longitude
            )
            refreshOnlineWorkersForEstate(firstEstate.name)
        }
    }

    fun applyPreselection(category: String?, workerId: Int?, workerName: String?) {
        var next = _uiState.value
        if (!category.isNullOrBlank()) {
            next = next.copy(selectedCategory = category, isCustomizerMode = true)
        }
        if (workerId != null && workerId != 0) {
            next = next.copy(
                matchingMode = "direct_booking",
                preferredWorkerId = workerId,
                preferredWorkerName = workerName ?: "Mama Fua #$workerId",
                isCustomizerMode = true
            )
        }
        _uiState.value = next
    }

    fun setBookingMode(customizer: Boolean) {
        _uiState.value = _uiState.value.copy(isCustomizerMode = customizer, errorMessage = null)
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateServiceQuantity(code: String, delta: Int) {
        val current = _uiState.value.itemQuantities[code] ?: 0
        val nextQty = (current + delta).coerceIn(0, 20)
        val updated = _uiState.value.itemQuantities.toMutableMap()
        if (nextQty == 0) updated.remove(code) else updated[code] = nextQty
        _uiState.value = _uiState.value.copy(itemQuantities = updated)
        recalculateEstimate()
    }

    fun updateAddonQuantity(code: String, delta: Int) {
        val current = _uiState.value.addonQuantities[code] ?: 0
        val nextQty = (current + delta).coerceIn(0, 20)
        val updated = _uiState.value.addonQuantities.toMutableMap()
        if (nextQty == 0) updated.remove(code) else updated[code] = nextQty
        _uiState.value = _uiState.value.copy(addonQuantities = updated)
        recalculateEstimate()
    }

    fun toggleDetergents(workerBrings: Boolean) {
        _uiState.value = _uiState.value.copy(workerBringsDetergents = workerBrings)
        recalculateEstimate()
    }

    private fun recalculateEstimate() {
        val state = _uiState.value
        val items = state.itemQuantities.map { SelectedQuantityItem(it.key, it.value) }
        val addons = state.addonQuantities.map { SelectedQuantityItem(it.key, it.value) }

        // Instant local preview first so UI feels zero-latency
        val localPreview = EldoretCatalogDefaults.computeLocalEstimate(
            catalog = state.catalog,
            items = items,
            addons = addons,
            workerBringsDetergents = state.workerBringsDetergents
        )
        _uiState.value = state.copy(estimate = localPreview, isEstimating = true)

        estimateJob?.cancel()
        estimateJob = viewModelScope.launch {
            delay(250)
            val remoteEstimate = catalogRepository.estimatePrice(
                items = items,
                addons = addons,
                workerBringsDetergents = _uiState.value.workerBringsDetergents
            )
            _uiState.value = _uiState.value.copy(
                estimate = remoteEstimate,
                isEstimating = false
            )
        }
    }

    fun selectEstate(estate: EldoretEstate) {
        _uiState.value = _uiState.value.copy(
            selectedEstate = estate,
            pinnedLatitude = estate.latitude,
            pinnedLongitude = estate.longitude
        )
        refreshOnlineWorkersForEstate(estate.name)
    }

    fun updatePinnedLocation(
        lat: Double,
        lng: Double,
        nearestEstate: EldoretEstate,
        resolvedAddress: String?
    ) {
        val prevEstate = _uiState.value.selectedEstate.name
        _uiState.value = _uiState.value.copy(
            pinnedLatitude = lat,
            pinnedLongitude = lng,
            selectedEstate = nearestEstate
        )
        if (!resolvedAddress.isNullOrBlank()) {
            location = resolvedAddress
        }
        if (!prevEstate.equals(nearestEstate.name, ignoreCase = true)) {
            refreshOnlineWorkersForEstate(nearestEstate.name)
        }
    }

    private fun refreshOnlineWorkersForEstate(estateName: String) {
        viewModelScope.launch {
            val res = workerRepository.getWorkers(estate = estateName, availabilityStatus = "online")
            val list = (res as? Resource.Success)?.data
                ?.ifEmpty { (workerRepository.getWorkers(availabilityStatus = "online") as? Resource.Success)?.data ?: emptyList() }
                ?: emptyList()
            _uiState.value = _uiState.value.copy(availableOnlineWorkers = list)
        }
    }

    fun setInstantBooking(instant: Boolean) {
        _uiState.value = _uiState.value.copy(isInstant = instant)
    }

    fun updateSchedule(date: String, time: String) {
        _uiState.value = _uiState.value.copy(scheduledDate = date, scheduledTime = time)
    }

    fun selectMatchingMode(mode: String) {
        _uiState.value = _uiState.value.copy(matchingMode = mode)
    }

    fun selectPreferredWorker(worker: WorkerProfile) {
        _uiState.value = _uiState.value.copy(
            matchingMode = "direct_booking",
            preferredWorkerId = worker.resolvedUserId,
            preferredWorkerName = worker.displayName
        )
    }

    fun submitBooking(onBookingCreated: (jobId: Int) -> Unit) {
        val state = _uiState.value
        val fullLocation = if (location.isBlank()) {
            "${state.selectedEstate.name}, Eldoret"
        } else {
            "${location.trim()}, ${state.selectedEstate.name}"
        }

        if (state.isCustomizerMode) {
            val items = state.itemQuantities.filterValues { it > 0 }
                .map { SelectedQuantityItem(it.key, it.value) }
            val addons = state.addonQuantities.filterValues { it > 0 }
                .map { SelectedQuantityItem(it.key, it.value) }

            if (items.isEmpty()) {
                _uiState.value = state.copy(errorMessage = "Please select at least 1 cleaning or laundry item.")
                return
            }
            if (state.matchingMode == "direct_booking" && state.preferredWorkerId == null) {
                _uiState.value = state.copy(errorMessage = "Please choose a Mama Fua for Direct Booking, or switch to Auto-Dispatch.")
                return
            }

            val scheduledIso = if (state.isInstant) null
            else "${state.scheduledDate.trim()}T${state.scheduledTime.trim()}:00.000Z"

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
                val req = CreateJobRequest(
                    location = fullLocation,
                    estate = state.selectedEstate.name,
                    latitude = state.pinnedLatitude,
                    longitude = state.pinnedLongitude,
                    isInstant = state.isInstant,
                    scheduledAt = scheduledIso,
                    workerBringsDetergents = state.workerBringsDetergents,
                    matchingMode = state.matchingMode,
                    preferredWorkerId = if (state.matchingMode == "direct_booking") state.preferredWorkerId else null,
                    items = items,
                    addons = addons
                )
                when (val result = jobRepository.createCustomJob(req)) {
                    is Resource.Success -> {
                        val createdId = result.data?.id ?: 0
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Booking created! Proceed to M-Pesa Escrow."
                        )
                        onBookingCreated(createdId)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Failed to create booking"
                        )
                    }
                    else -> _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        } else {
            // Manual custom job
            if (title.isBlank() || description.isBlank() || budget.isBlank()) {
                _uiState.value = state.copy(errorMessage = "Please fill in Title, Description, and Budget")
                return
            }
            val budgetDouble = budget.toDoubleOrNull()
            if (budgetDouble == null || budgetDouble <= 0.0) {
                _uiState.value = state.copy(errorMessage = "Please enter a valid KES budget")
                return
            }
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val req = CreateJobRequest(
                    title = title.trim(),
                    description = description.trim(),
                    location = fullLocation,
                    estate = state.selectedEstate.name,
                    latitude = state.pinnedLatitude,
                    longitude = state.pinnedLongitude,
                    budget = budgetDouble,
                    isInstant = state.isInstant,
                    matchingMode = state.matchingMode,
                    preferredWorkerId = if (state.matchingMode == "direct_booking") state.preferredWorkerId else null
                )
                when (val result = jobRepository.createCustomJob(req)) {
                    is Resource.Success -> {
                        val createdId = result.data?.id ?: 0
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Job posted successfully!"
                        )
                        onBookingCreated(createdId)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Failed to post job"
                        )
                    }
                    else -> _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
