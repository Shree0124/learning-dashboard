package com.learning.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learning.dashboard.domain.model.Course

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessonsCount: Int
) {
    fun toDomain(): Course {
        return Course(
            id = id,
            title = title,
            instructor = instructor,
            progress = progress,
            lessonsCount = lessonsCount,
            lessons = emptyList()
        )
    }

    companion object {
        fun fromDomain(course: Course): CourseEntity {
            return CourseEntity(
                id = course.id,
                title = course.title,
                instructor = course.instructor,
                progress = course.progress,
                lessonsCount = course.lessonsCount
            )
        }
    }
}
