package com.learning.dashboard.presentation.dashboard

import com.learning.dashboard.domain.model.Course

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val courses: List<Course>, val isOffline: Boolean) : DashboardUiState
    data class Empty(val isOffline: Boolean) : DashboardUiState
    data class Error(val message: String, val cachedCourses: List<Course> = emptyList()) : DashboardUiState
}
