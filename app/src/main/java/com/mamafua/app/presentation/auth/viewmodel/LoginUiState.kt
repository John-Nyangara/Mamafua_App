package com.mamafua.app.presentation.auth.viewmodel

data class LoginUiState(
    val email: String = "client@mamafua.com",
    val password: String = "password123",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
