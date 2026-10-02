package com.learning.dashboard.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.dashboard.domain.usecase.GetCourseDetailUseCase
import com.learning.dashboard.domain.usecase.ToggleLessonCompletionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourseDetailViewModel(
    private val courseId: Int,
    private val getCourseDetailUseCase: GetCourseDetailUseCase,
    private val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        observeCourse()
    }

    private fun observeCourse() {
        viewModelScope.launch {
            getCourseDetailUseCase(courseId).collect { course ->
                _uiState.update {
                    if (course != null) {
                        CourseDetailUiState.Success(course)
                    } else {
                        CourseDetailUiState.Error("Course not found.")
                    }
                }
            }
        }
    }

    fun toggleLesson(lessonId: Int) {
        viewModelScope.launch {
            toggleLessonCompletionUseCase(courseId, lessonId)
        }
    }

    class Factory(
        private val courseId: Int,
        private val getCourseDetailUseCase: GetCourseDetailUseCase,
        private val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseDetailViewModel(courseId, getCourseDetailUseCase, toggleLessonCompletionUseCase) as T
        }
    }
}
