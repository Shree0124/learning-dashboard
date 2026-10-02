package com.learning.dashboard.domain.repository

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getCoursesStream(): Flow<Resource<List<Course>>>
    suspend fun refreshCourses(): Result<Unit>
    fun getCourseStream(courseId: Int): Flow<Course?>
    suspend fun toggleLessonCompletion(courseId: Int, lessonId: Int): Result<Unit>
    fun observeNetworkConnectivity(): Flow<Boolean>
    fun isConnected(): Boolean
}
