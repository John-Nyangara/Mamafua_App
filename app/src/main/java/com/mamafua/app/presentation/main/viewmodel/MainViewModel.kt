package com.mamafua.app.presentation.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository
) : ViewModel() {

    val authState = userRepository.getUserFlow().map { user ->
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty() || user == null) {
            AuthState.Unauthenticated
        } else {
            if (user.role.equals("CLIENT", ignoreCase = true)) AuthState.AuthenticatedClient else AuthState.AuthenticatedWorker
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object AuthenticatedClient : AuthState()
    object AuthenticatedWorker : AuthState()
}
