package com.learning.dashboard.presentation.login

data class LoginUiState(
    val email: String = "john.doe@example.com",
    val password: String = "password123",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isLoginSuccessful: Boolean = false
)
