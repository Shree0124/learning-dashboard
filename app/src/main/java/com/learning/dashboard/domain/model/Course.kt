package com.learning.dashboard.domain.model

data class Course(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessonsCount: Int,
    val lessons: List<Lesson> = emptyList()
) {
    val completedLessonsCount: Int
        get() = lessons.count { it.isCompleted }

    val calculatedProgress: Int
        get() = if (lessonsCount > 0) {
            val total = if (lessons.isNotEmpty()) lessons.size else lessonsCount
            val completed = completedLessonsCount
            ((completed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            progress.coerceIn(0, 100)
        }
}
