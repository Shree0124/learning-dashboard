package com.learning.dashboard.data.remote.dto

data class CourseDto(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessons: Int,
    val lessonItems: List<LessonDto> = emptyList()
)
