package com.mamafua.app.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> _uiState.value = _uiState.value.copy(name = event.name)
            is RegisterEvent.EmailChanged -> _uiState.value = _uiState.value.copy(email = event.email)
            is RegisterEvent.PhoneChanged -> _uiState.value = _uiState.value.copy(phone = event.phone)
            is RegisterEvent.PasswordChanged -> _uiState.value = _uiState.value.copy(password = event.password)
            is RegisterEvent.RoleChanged -> _uiState.value = _uiState.value.copy(role = event.role)
            RegisterEvent.Submit -> register()
            RegisterEvent.DismissError -> _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun register() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.phone.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.register(state.name, state.email, state.phone, state.password, state.role)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "An unexpected error occurred")
            }
        }
    }
}
