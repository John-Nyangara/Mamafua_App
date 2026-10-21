package com.mamafua.app.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _uiState.value = _uiState.value.copy(email = event.email)
            is LoginEvent.PasswordChanged -> _uiState.value = _uiState.value.copy(password = event.password)
            LoginEvent.Submit -> login()
            LoginEvent.DismissError -> _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.login(state.email, state.password)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Error during login", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "An unexpected error occurred")
            }
        }
    }
}
