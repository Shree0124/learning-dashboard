package com.learning.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.learning.dashboard.domain.model.Lesson

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["courseId", "orderIndex"])
    ]
)
data class LessonEntity(
    @PrimaryKey
    val id: Int,
    val courseId: Int,
    val title: String,
    val isCompleted: Boolean,
    val orderIndex: Int
) {
    fun toDomain(): Lesson {
        return Lesson(
            id = id,
            courseId = courseId,
            title = title,
            isCompleted = isCompleted,
            orderIndex = orderIndex
        )
    }

    companion object {
        fun fromDomain(lesson: Lesson): LessonEntity {
            return LessonEntity(
                id = lesson.id,
                courseId = lesson.courseId,
                title = lesson.title,
                isCompleted = lesson.isCompleted,
                orderIndex = lesson.orderIndex
            )
        }
    }
}
