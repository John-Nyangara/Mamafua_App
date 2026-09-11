package com.mamafua.app.presentation.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.model.WorkerProfile
import com.mamafua.app.data.remote.api.WorkerApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientHomeViewModel(
    private val workerApi: WorkerApiService
) : ViewModel() {
    private val _workers = MutableStateFlow<List<WorkerProfile>>(emptyList())
    val workers = _workers.asStateFlow()

    fun searchWorkers(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val results = workerApi.searchWorkers(lat, lng, 10.0)
                _workers.value = results
            } catch (e: Exception) {
                // Ignore error for now
            }
        }
    }
}
