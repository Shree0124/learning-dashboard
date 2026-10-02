package com.learning.dashboard.presentation.details

import com.learning.dashboard.domain.model.Course

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Success(val course: Course) : CourseDetailUiState
    data class Error(val message: String) : CourseDetailUiState
}
