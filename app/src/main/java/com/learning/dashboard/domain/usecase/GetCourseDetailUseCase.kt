package com.learning.dashboard.domain.usecase

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

class GetCourseDetailUseCase(
    private val repository: CourseRepository
) {
    operator fun invoke(courseId: Int): Flow<Course?> {
        return repository.getCourseStream(courseId)
    }
}
