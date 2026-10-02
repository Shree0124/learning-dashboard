package com.learning.dashboard.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(loginUseCase.isConnected())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        viewModelScope.launch {
            loginUseCase.observeNetworkConnectivity().collect { connected ->
                _isConnected.value = connected
                if (connected && _uiState.value.generalError?.contains("internet", ignoreCase = true) == true) {
                    _uiState.update { it.copy(generalError = null) }
                }
            }
        }
    }

    fun onEmailChanged(newEmail: String) {
        _uiState.update {
            it.copy(
                email = newEmail,
                emailError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update {
            it.copy(
                password = newPassword,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun login() {
        val currentState = _uiState.value

        // Check device internet connectivity
        if (!loginUseCase.isConnected()) {
            _uiState.update {
                it.copy(
                    generalError = "No internet connection. An active internet connection is required to log in.",
                    isLoading = false
                )
            }
            return
        }

        // Validate credentials
        val validation = loginUseCase.validateInput(currentState.email, currentState.password)
        if (validation is LoginUseCase.ValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    emailError = validation.emailError,
                    passwordError = validation.passwordError
                )
            }
            return
        }

        // Set Loading state
        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = loginUseCase.execute(currentState.email, currentState.password)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            generalError = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message
                        )
                    }
                }
                Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isLoginSuccessful = false) }
    }

    class Factory(private val loginUseCase: LoginUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(loginUseCase) as T
        }
    }
}
