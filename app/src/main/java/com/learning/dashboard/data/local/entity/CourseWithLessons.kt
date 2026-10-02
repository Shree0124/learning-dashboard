package com.learning.dashboard.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.learning.dashboard.domain.model.Course

data class CourseWithLessons(
    @Embedded
    val course: CourseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val lessons: List<LessonEntity>
) {
    fun toDomain(): Course {
        val sortedLessons = lessons.sortedBy { it.orderIndex }.map { it.toDomain() }
        return Course(
            id = course.id,
            title = course.title,
            instructor = course.instructor,
            progress = course.progress,
            lessonsCount = course.lessonsCount,
            lessons = sortedLessons
        )
    }
}
