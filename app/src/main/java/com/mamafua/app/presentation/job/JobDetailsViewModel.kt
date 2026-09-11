package com.mamafua.app.presentation.job

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.repository.JobRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JobDetailsViewModel(
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = checkNotNull(savedStateHandle["jobId"]?.toString()?.toIntOrNull())

    val job: StateFlow<Job?> = jobRepository.getJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun applyForJob() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                jobRepository.applyForJob(jobId)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptWorker(workerId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                jobRepository.acceptWorker(jobId, workerId)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                jobRepository.updateJobStatus(jobId, status)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
