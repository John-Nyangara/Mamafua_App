package com.mamafua.app.presentation.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientJobApplicantsViewModel(
    private val jobId: Int,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicantsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadApplicants()
    }

    fun loadApplicants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = jobRepository.getJobApplicants(jobId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        applicants = result.data ?: emptyList()
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to load applicants"
                    )
                }
                else -> Unit
            }
        }
    }

    fun shortlistApplicant(workerId: Int) {
        viewModelScope.launch {
            when (val result = jobRepository.shortlistWorker(jobId, workerId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Applicant shortlisted!")
                    loadApplicants()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message ?: "Failed to shortlist")
                }
                else -> Unit
            }
        }
    }

    fun acceptApplicant(workerId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = jobRepository.acceptWorker(jobId, workerId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Mama Fua accepted! 4-digit Start PIN generated.",
                        acceptedWorker = true
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to accept worker"
                    )
                }
                else -> Unit
            }
        }
    }

    fun rejectApplicant(workerId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = jobRepository.rejectWorker(jobId, workerId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Applicant rejected"
                    )
                    loadApplicants()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to reject worker"
                    )
                }
                else -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null, acceptedWorker = false)
    }
}

data class ApplicantsUiState(
    val isLoading: Boolean = false,
    val applicants: List<WorkerProfile> = emptyList(),
    val successMessage: String? = null,
    val error: String? = null,
    val acceptedWorker: Boolean = false
)
