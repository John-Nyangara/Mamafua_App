package com.mamafua.app.presentation.auth.viewmodel

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val role: String = "Client",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
