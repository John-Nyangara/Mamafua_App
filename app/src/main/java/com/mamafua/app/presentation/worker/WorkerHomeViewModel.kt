package com.mamafua.app.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.JobStatus
import com.mamafua.app.domain.model.RadarOffer
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WorkerFeedState {
    object Loading : WorkerFeedState()
    data class Success(val jobs: List<Job>) : WorkerFeedState()
    data class Error(val message: String) : WorkerFeedState()
}

class WorkerHomeViewModel(
    private val jobRepository: JobRepository,
    private val workerRepository: WorkerRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _feedState = MutableStateFlow<WorkerFeedState>(WorkerFeedState.Loading)
    val feedState: StateFlow<WorkerFeedState> = _feedState.asStateFlow()

    private val _radarOffers = MutableStateFlow<List<RadarOffer>>(emptyList())
    val radarOffers: StateFlow<List<RadarOffer>> = _radarOffers.asStateFlow()

    private val _activeJob = MutableStateFlow<Job?>(null)
    val activeJob: StateFlow<Job?> = _activeJob.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _selectedEstate = MutableStateFlow<String?>(null)
    val selectedEstate: StateFlow<String?> = _selectedEstate.asStateFlow()

    private val _workerLatitude = MutableStateFlow(0.5143)
    val workerLatitude: StateFlow<Double> = _workerLatitude.asStateFlow()

    private val _workerLongitude = MutableStateFlow(35.2698)
    val workerLongitude: StateFlow<Double> = _workerLongitude.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        val savedStatus = tokenManager.getAvailabilityStatus()
        if (!savedStatus.isNullOrBlank()) {
            _isOnline.value = savedStatus.equals("online", ignoreCase = true) ||
                savedStatus.equals("available", ignoreCase = true)
        }
        refreshAll()
    }

    fun refreshAll() {
        loadActiveJob()
        loadOpenJobs()
        loadRadarOffers()
    }

    fun loadActiveJob() {
        viewModelScope.launch {
            when (val res = jobRepository.fetchMyAssignedJobs()) {
                is Resource.Success -> {
                    val active = res.data?.firstOrNull {
                        it.status in listOf(
                            JobStatus.ASSIGNED,
                            JobStatus.ACCEPTED,
                            JobStatus.ARRIVED,
                            JobStatus.IN_PROGRESS,
                            JobStatus.COMPLETION_REQUESTED
                        )
                    }
                    _activeJob.value = active
                }
                else -> Unit
            }
        }
    }

    fun toggleOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            val prev = _isOnline.value
            val statusStr = if (online) "online" else "offline"
            _isOnline.value = online
            when (val res = workerRepository.toggleAvailability(statusStr)) {
                is Resource.Success -> {
                    _actionMessage.value = if (online) {
                        "You are now ONLINE for Eldoret Instant Radar dispatches!"
                    } else {
                        "You are now OFFLINE."
                    }
                    if (online) {
                        loadRadarOffers()
                    }
                }
                is Resource.Error -> {
                    _isOnline.value = prev
                    _actionMessage.value = "Could not toggle status: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun loadRadarOffers() {
        viewModelScope.launch {
            when (val res = workerRepository.getJobRadar()) {
                is Resource.Success -> {
                    _radarOffers.value = res.data.orEmpty()
                }
                else -> Unit
            }
        }
    }

    fun respondToRadar(offerId: Int, jobId: Int, action: String, onAccepted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            when (val res = workerRepository.respondToRadarOffer(offerId, action)) {
                is Resource.Success -> {
                    _actionMessage.value = if (action == "accept") {
                        "Job locked! Opening active checklist..."
                    } else {
                        "Radar offer declined."
                    }
                    loadRadarOffers()
                    loadOpenJobs()
                    if (action == "accept" && jobId > 0) {
                        onAccepted(jobId)
                    }
                }
                is Resource.Error -> {
                    _actionMessage.value = "Radar response failed: ${res.message}"
                    loadRadarOffers()
                }
                else -> Unit
            }
        }
    }

    fun selectEstateFilter(estate: String?) {
        _selectedEstate.value = estate
        if (estate != null) {
            com.mamafua.app.domain.model.EldoretCatalogDefaults.defaultEstates.find {
                it.name.equals(estate, ignoreCase = true)
            }?.let { est ->
                _workerLatitude.value = est.latitude
                _workerLongitude.value = est.longitude
            }
        }
        loadOpenJobs()
    }

    fun updateWorkerRadarCenter(lat: Double, lng: Double, nearestEstateName: String?) {
        _workerLatitude.value = lat
        _workerLongitude.value = lng
    }

    fun loadOpenJobs() {
        viewModelScope.launch {
            _feedState.value = WorkerFeedState.Loading
            when (val res = jobRepository.fetchJobs(estate = _selectedEstate.value)) {
                is Resource.Success -> {
                    _feedState.value = WorkerFeedState.Success(res.data.orEmpty())
                }
                is Resource.Error -> {
                    _feedState.value = WorkerFeedState.Error(res.message ?: "Failed to load jobs")
                }
                else -> Unit
            }
        }
    }

    fun applyForJob(jobId: Int) {
        viewModelScope.launch {
            when (val res = jobRepository.applyForJob(jobId)) {
                is Resource.Success -> {
                    _actionMessage.value = "Applied for job! Client will be notified."
                    loadOpenJobs()
                }
                is Resource.Error -> {
                    _actionMessage.value = "Failed to apply: ${res.message}"
                }
                else -> Unit
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
