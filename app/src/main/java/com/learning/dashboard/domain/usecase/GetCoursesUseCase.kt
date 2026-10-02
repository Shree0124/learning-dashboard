package com.learning.dashboard.domain.usecase

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

class GetCoursesUseCase(
    private val repository: CourseRepository
) {
    operator fun invoke(): Flow<Resource<List<Course>>> {
        return repository.getCoursesStream()
    }

    suspend fun refresh(): Result<Unit> {
        return repository.refreshCourses()
    }

    fun observeNetworkConnectivity(): Flow<Boolean> {
        return repository.observeNetworkConnectivity()
    }

    fun isConnected(): Boolean {
        return repository.isConnected()
    }
}
