package com.mamafua.app.presentation.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.data.remote.MamaFuaApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class ClientHomeUiState(
    val isLoading: Boolean = false,
    val workers: List<WorkerProfile> = emptyList(),
    val errorMessage: String? = null
)

class ClientHomeViewModel(
    private val apiClient: MamaFuaApiClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientHomeUiState())
    val uiState = _uiState.asStateFlow()

    val workers = _uiState.map { it.workers }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun searchWorkers(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiClient.getWorkers().getOrThrow()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    workers = result.workers
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load workers"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
