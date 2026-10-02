package com.learning.dashboard.data.remote

import com.learning.dashboard.data.remote.dto.CourseDto

interface CourseApiService {
    suspend fun getCourses(): List<CourseDto>
    suspend fun getCourseDetails(courseId: Int): CourseDto?
    var forceSimulateError: Boolean
    var isSimulatedOffline: Boolean
}
