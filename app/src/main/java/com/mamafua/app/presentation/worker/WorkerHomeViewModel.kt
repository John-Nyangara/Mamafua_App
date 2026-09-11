package com.mamafua.app.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.JobRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkerHomeViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {
    
    val availableJobs = jobRepository.getActiveJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                jobRepository.fetchJobs("OPEN")
            } catch (e: Exception) {
                // Ignore error for now
            }
        }
    }
}
