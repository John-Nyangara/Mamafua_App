package com.mamafua.app.presentation.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.CatalogData
import com.mamafua.app.domain.model.EldoretCatalogDefaults
import com.mamafua.app.domain.model.AvailabilitySlot
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.domain.model.WorkerReviewsSummary
import com.mamafua.app.domain.repository.CatalogRepository
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SelectedWorkerDetails(
    val worker: WorkerProfile,
    val reviewsSummary: WorkerReviewsSummary = WorkerReviewsSummary(),
    val availability: List<AvailabilitySlot> = emptyList(),
    val isLoadingDetails: Boolean = false
)

data class ClientHomeUiState(
    val isLoading: Boolean = false,
    val isRebooking: Boolean = false,
    val catalog: CatalogData = EldoretCatalogDefaults.defaultCatalog(),
    val favorites: List<WorkerProfile> = emptyList(),
    val workers: List<WorkerProfile> = emptyList(),
    val activeJob: Job? = null,
    val selectedEstate: String? = null,
    val selectedBadge: String? = null,
    val onlineOnly: Boolean = false,
    val selectedWorkerDetails: SelectedWorkerDetails? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ClientHomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val workerRepository: WorkerRepository,
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val catalog = catalogRepository.getCatalog()
            _uiState.value = _uiState.value.copy(catalog = catalog)
            refreshActiveJob()
            refreshFavorites()
            refreshWorkers()
        }
    }

    fun refreshActiveJob() {
        viewModelScope.launch {
            when (val res = jobRepository.fetchMyPostedJobs()) {
                is Resource.Success -> {
                    val active = res.data?.firstOrNull {
                        it.status in listOf(
                            JobStatus.OPEN,
                            JobStatus.ASSIGNED,
                            JobStatus.ACCEPTED,
                            JobStatus.ARRIVED,
                            JobStatus.IN_PROGRESS,
                            JobStatus.COMPLETION_REQUESTED
                        )
                    }
                    _uiState.value = _uiState.value.copy(activeJob = active)
                }
                else -> Unit
            }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            when (val favRes = userRepository.getFavorites()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(favorites = favRes.data ?: emptyList())
                }
                else -> Unit
            }
        }
    }

    fun refreshWorkers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val state = _uiState.value
            val estateObj = state.catalog.estates.find { it.name == state.selectedEstate }
            when (val res = workerRepository.getWorkers(
                estate = state.selectedEstate,
                badge = state.selectedBadge,
                availabilityStatus = if (state.onlineOnly) "online" else null,
                lat = estateObj?.latitude,
                lng = estateObj?.longitude,
                radiusKm = if (estateObj != null) 5.0 else null
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        workers = res.data ?: emptyList()
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectEstateFilter(estate: String?) {
        _uiState.value = _uiState.value.copy(selectedEstate = estate)
        refreshWorkers()
    }

    fun selectBadgeFilter(badge: String?) {
        _uiState.value = _uiState.value.copy(selectedBadge = badge)
        refreshWorkers()
    }

    fun toggleOnlineOnly(online: Boolean) {
        _uiState.value = _uiState.value.copy(onlineOnly = online)
        refreshWorkers()
    }

    fun rebookLastMamaFua(onRebooked: (Int) -> Unit) {
        rebookWorker(workerId = null, isInstant = true, scheduledAt = null, onRebooked = onRebooked)
    }

    fun rebookWorker(
        workerId: Int?,
        isInstant: Boolean,
        scheduledAt: String?,
        onRebooked: (Int) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRebooking = true, errorMessage = null)
            when (val res = jobRepository.rebookJob(workerId, isInstant, scheduledAt)) {
                is Resource.Success -> {
                    val createdJob = res.data
                    _uiState.value = _uiState.value.copy(
                        isRebooking = false,
                        successMessage = "15-minute Direct Booking Offer sent!"
                    )
                    if (createdJob != null && createdJob.id != 0) {
                        onRebooked(createdJob.id)
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRebooking = false,
                        errorMessage = res.message ?: "No previous completed booking found to rebook."
                    )
                }
                else -> _uiState.value = _uiState.value.copy(isRebooking = false)
            }
        }
    }

    fun toggleFavorite(worker: WorkerProfile) {
        val targetId = worker.resolvedUserId
        val isFav = _uiState.value.favorites.any { it.resolvedUserId == targetId || it.resolvedWorkerId == worker.resolvedWorkerId }
        viewModelScope.launch {
            if (isFav) {
                when (val res = userRepository.removeFavorite(targetId)) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(successMessage = "Removed ${worker.displayName} from favorites")
                        refreshFavorites()
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                    else -> Unit
                }
            } else {
                when (val res = userRepository.addFavorite(targetId, "Favorite Mama Fua")) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(successMessage = "Added ${worker.displayName} to My Favorites!")
                        refreshFavorites()
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                    else -> Unit
                }
            }
        }
    }

    fun openWorkerDetails(worker: WorkerProfile) {
        _uiState.value = _uiState.value.copy(
            selectedWorkerDetails = SelectedWorkerDetails(worker = worker, isLoadingDetails = true)
        )
        viewModelScope.launch {
            val detailWorker = (workerRepository.getWorkerById(worker.resolvedWorkerId) as? Resource.Success)?.data ?: worker
            val reviews = (workerRepository.getWorkerReviews(worker.resolvedWorkerId) as? Resource.Success)?.data
                ?: WorkerReviewsSummary()
            val schedule = (workerRepository.getWorkerAvailability(worker.resolvedWorkerId) as? Resource.Success)?.data
                ?: emptyList()
            _uiState.value = _uiState.value.copy(
                selectedWorkerDetails = SelectedWorkerDetails(
                    worker = detailWorker,
                    reviewsSummary = reviews,
                    availability = schedule,
                    isLoadingDetails = false
                )
            )
        }
    }

    fun dismissWorkerDetails() {
        _uiState.value = _uiState.value.copy(selectedWorkerDetails = null)
    }

    fun reportWorker(userId: Int, reason: String) {
        viewModelScope.launch {
            when (val res = userRepository.reportUser(userId, reason)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Report submitted to Trust & Safety team.",
                        selectedWorkerDetails = null
                    )
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                else -> Unit
            }
        }
    }

    fun blockWorker(userId: Int) {
        viewModelScope.launch {
            when (val res = userRepository.blockUser(userId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "User blocked.",
                        selectedWorkerDetails = null
                    )
                    refreshWorkers()
                    refreshFavorites()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                else -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
