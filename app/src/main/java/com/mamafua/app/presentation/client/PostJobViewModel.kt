package com.mamafua.app.presentation.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostJobUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class PostJobViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf("")
    var budget by mutableStateOf("")

    private val _uiState = MutableStateFlow(PostJobUiState())
    val uiState = _uiState.asStateFlow()

    fun postJob(onJobPosted: () -> Unit) {
        if (title.isBlank() || description.isBlank() || location.isBlank() || budget.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }

        val budgetDouble = budget.toDoubleOrNull()
        if (budgetDouble == null || budgetDouble <= 0.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid budget")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            
            val result = jobRepository.createJob(
                title = title,
                description = description,
                location = location,
                budget = budgetDouble
            )
            
            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Job posted successfully!"
                    )
                    onJobPosted()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Failed to post job. Please try again."
                    )
                }
                else -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
