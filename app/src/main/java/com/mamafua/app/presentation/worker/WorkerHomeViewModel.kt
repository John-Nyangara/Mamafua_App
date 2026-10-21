package com.mamafua.app.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.use_case.ApplyForJobUseCase
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine

class WorkerHomeViewModel(
    private val jobRepository: JobRepository,
    private val tokenManager: TokenManager,
    private val applyForJobUseCase: ApplyForJobUseCase = ApplyForJobUseCase(jobRepository, tokenManager)
) : ViewModel() {
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
        WorkerHomeUiState(appliedJobs = tokenManager.getAppliedJobs())
    )

    val uiState = combine(_uiState, jobRepository.getActiveJobs()) { state, jobs ->
        state.copy(availableJobs = jobs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkerHomeUiState(appliedJobs = tokenManager.getAppliedJobs()))

    init {
        fetchJobs()
    }

    fun fetchJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = jobRepository.fetchJobs("OPEN")
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun applyForJob(jobId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = applyForJobUseCase(jobId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Successfully applied for job!",
                        appliedJobs = tokenManager.getAppliedJobs()
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null)
    }
}

data class WorkerHomeUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val appliedJobs: Set<Int> = emptySet(),
    val availableJobs: List<com.mamafua.app.domain.model.Job> = emptyList()
)
