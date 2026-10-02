package com.learning.dashboard.domain.usecase

import com.learning.dashboard.domain.repository.CourseRepository

class ToggleLessonCompletionUseCase(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(courseId: Int, lessonId: Int): Result<Unit> {
        return repository.toggleLessonCompletion(courseId, lessonId)
    }
}
