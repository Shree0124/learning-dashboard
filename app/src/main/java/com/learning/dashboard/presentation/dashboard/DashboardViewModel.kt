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

    private val _isOfflineSimulated = MutableStateFlow(getCoursesUseCase.isSimulatedOffline())
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            getCoursesUseCase().collect { resource ->
                val isOffline = _isOfflineSimulated.value
                _uiState.update {
                    when (resource) {
                        is Resource.Loading -> {
                            // If we already have courses, keep them showing; otherwise show loading
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
                                // Graceful fallback: show cached courses with offline warning
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

    fun toggleOfflineSimulation() {
        val newStatus = !_isOfflineSimulated.value
        _isOfflineSimulated.value = newStatus
        getCoursesUseCase.setSimulatedOffline(newStatus)

        // If toggled offline, update state to show offline indicator
        val current = _uiState.value
        if (current is DashboardUiState.Success) {
            _uiState.update { current.copy(isOffline = newStatus) }
        } else if (current is DashboardUiState.Empty) {
            _uiState.update { current.copy(isOffline = newStatus) }
        } else if (!newStatus) {
            // Toggled back online -> trigger refresh
            refresh()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
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
