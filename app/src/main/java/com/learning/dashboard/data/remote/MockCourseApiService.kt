package com.learning.dashboard.data.remote

import com.learning.dashboard.data.remote.dto.CourseDto
import com.learning.dashboard.data.remote.dto.LessonDto
import kotlinx.coroutines.delay
import java.io.IOException

class MockCourseApiService(
    private val simulatedLatencyMs: Long = 600L
) : CourseApiService {

    override var forceSimulateError: Boolean = false
    override var isSimulatedOffline: Boolean = false

    private val mockCourses = listOf(
        CourseDto(
            id = 1,
            title = "Python Programming",
            instructor = "John Smith",
            progress = 65,
            lessons = 20,
            lessonItems = listOf(
                LessonDto(id = 101, courseId = 1, title = "Introduction", isCompleted = true, orderIndex = 1),
                LessonDto(id = 102, courseId = 1, title = "Variables & Data Types", isCompleted = true, orderIndex = 2),
                LessonDto(id = 103, courseId = 1, title = "Functions", isCompleted = false, orderIndex = 3),
                LessonDto(id = 104, courseId = 1, title = "OOP", isCompleted = false, orderIndex = 4),
                LessonDto(id = 105, courseId = 1, title = "Modules & Packages", isCompleted = false, orderIndex = 5),
                LessonDto(id = 106, courseId = 1, title = "File I/O & Exceptions", isCompleted = false, orderIndex = 6)
            )
        ),
        CourseDto(
            id = 2,
            title = "Generative AI",
            instructor = "Sarah Williams",
            progress = 40,
            lessons = 16,
            lessonItems = listOf(
                LessonDto(id = 201, courseId = 2, title = "Introduction to LLMs", isCompleted = true, orderIndex = 1),
                LessonDto(id = 202, courseId = 2, title = "Prompt Engineering Foundations", isCompleted = true, orderIndex = 2),
                LessonDto(id = 203, courseId = 2, title = "Embeddings & Vector Databases", isCompleted = false, orderIndex = 3),
                LessonDto(id = 204, courseId = 2, title = "RAG System Architecture", isCompleted = false, orderIndex = 4),
                LessonDto(id = 205, courseId = 2, title = "Fine-Tuning & Evaluation", isCompleted = false, orderIndex = 5)
            )
        ),
        CourseDto(
            id = 3,
            title = "Full Stack Development",
            instructor = "David Brown",
            progress = 25,
            lessons = 28,
            lessonItems = listOf(
                LessonDto(id = 301, courseId = 3, title = "HTML5 & Semantic Markup", isCompleted = true, orderIndex = 1),
                LessonDto(id = 302, courseId = 3, title = "Modern CSS & Flexbox", isCompleted = true, orderIndex = 2),
                LessonDto(id = 303, courseId = 3, title = "JavaScript ES6+ Deep Dive", isCompleted = false, orderIndex = 3),
                LessonDto(id = 304, courseId = 3, title = "REST APIs & JSON Handling", isCompleted = false, orderIndex = 4),
                LessonDto(id = 305, courseId = 3, title = "Database Design & SQL", isCompleted = false, orderIndex = 5),
                LessonDto(id = 306, courseId = 3, title = "Deployment & CI/CD", isCompleted = false, orderIndex = 6)
            )
        )
    )

    override suspend fun getCourses(): List<CourseDto> {
        if (simulatedLatencyMs > 0) {
            delay(simulatedLatencyMs)
        }
        if (isSimulatedOffline) {
            throw IOException("Network unavailable. No connection to remote learning servers.")
        }
        if (forceSimulateError) {
            throw IOException("Internal 500 Server Error: Unable to fetch courses.")
        }
        return mockCourses
    }

    override suspend fun getCourseDetails(courseId: Int): CourseDto? {
        if (simulatedLatencyMs > 0) {
            delay(simulatedLatencyMs)
        }
        if (isSimulatedOffline) {
            throw IOException("Network unavailable. Cannot fetch remote course details.")
        }
        if (forceSimulateError) {
            throw IOException("Internal 500 Server Error: Course details not found.")
        }
        return mockCourses.find { it.id == courseId }
    }
}
