package com.learning.dashboard.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.repository.AuthRepository
import com.learning.dashboard.domain.usecase.GetCoursesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(getCoursesUseCase.isConnected())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        observeConnectivity()
        loadCourses()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            getCoursesUseCase.observeNetworkConnectivity().collect { connected ->
                val wasDisconnected = !_isConnected.value
                _isConnected.value = connected

                // Update isOffline status in current UI state
                _uiState.update { current ->
                    when (current) {
                        is DashboardUiState.Success -> current.copy(isOffline = !connected)
                        is DashboardUiState.Empty -> current.copy(isOffline = !connected)
                        else -> current
                    }
                }

                // If internet was restored after being disconnected, automatically sync latest data
                if (wasDisconnected && connected) {
                    refresh()
                }
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            getCoursesUseCase().collect { resource ->
                val isOffline = !_isConnected.value
                _uiState.update {
                    when (resource) {
                        is Resource.Loading -> {
                            val currentCourses = (it as? DashboardUiState.Success)?.courses
                            if (currentCourses.isNullOrEmpty()) {
                                DashboardUiState.Loading
                            } else {
                                DashboardUiState.Success(currentCourses, isOffline)
                            }
                        }
                        is Resource.Success -> {
                            if (resource.data.isEmpty()) {
                                DashboardUiState.Empty(isOffline)
                            } else {
                                DashboardUiState.Success(resource.data, isOffline)
                            }
                        }
                        is Resource.Error -> {
                            val currentCourses = (it as? DashboardUiState.Success)?.courses ?: emptyList()
                            if (currentCourses.isNotEmpty()) {
                                DashboardUiState.Success(currentCourses, isOffline = true)
                            } else {
                                DashboardUiState.Error(
                                    message = resource.message,
                                    cachedCourses = currentCourses
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { DashboardUiState.Loading }
            val result = getCoursesUseCase.refresh()
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to refresh courses"
                val current = _uiState.value
                val existing = (current as? DashboardUiState.Success)?.courses ?: emptyList()
                if (existing.isNotEmpty()) {
                    _uiState.update { DashboardUiState.Success(existing, isOffline = true) }
                } else {
                    _uiState.update { DashboardUiState.Error(errorMsg) }
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit, onLogoutBlocked: (String) -> Unit) {
        if (!_isConnected.value) {
            onLogoutBlocked("Cannot log out while offline. An active internet connection is required.")
            return
        }
        viewModelScope.launch {
            val result = authRepository.logout()
            if (result.isSuccess) {
                onLoggedOut()
            } else {
                onLogoutBlocked(
                    result.exceptionOrNull()?.message ?: "Cannot log out while offline."
                )
            }
        }
    }

    class Factory(
        private val getCoursesUseCase: GetCoursesUseCase,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getCoursesUseCase, authRepository) as T
        }
    }
}
