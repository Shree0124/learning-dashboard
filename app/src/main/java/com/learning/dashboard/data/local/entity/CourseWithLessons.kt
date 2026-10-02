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
        val domainCourse = course.toDomain().copy(
            lessons = sortedLessons,
            lessonsCount = if (sortedLessons.isNotEmpty()) sortedLessons.size else course.lessonsCount
        )
        return domainCourse.copy(
            progress = domainCourse.calculatedProgress
        )
    }
}
