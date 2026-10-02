package com.learning.dashboard.data.remote.dto

data class LessonDto(
    val id: Int,
    val courseId: Int,
    val title: String,
    val isCompleted: Boolean,
    val orderIndex: Int
)
