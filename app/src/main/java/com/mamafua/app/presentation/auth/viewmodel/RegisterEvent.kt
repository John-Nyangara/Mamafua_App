package com.mamafua.app.presentation.auth.viewmodel

sealed class RegisterEvent {
    data class NameChanged(val name: String) : RegisterEvent()
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PhoneChanged(val phone: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class RoleChanged(val role: String) : RegisterEvent()
    object Submit : RegisterEvent()
    object DismissError : RegisterEvent()
}
