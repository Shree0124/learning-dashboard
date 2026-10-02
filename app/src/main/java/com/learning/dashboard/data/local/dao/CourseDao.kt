package com.learning.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.learning.dashboard.data.local.entity.CourseEntity
import com.learning.dashboard.data.local.entity.CourseWithLessons
import com.learning.dashboard.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Transaction
    @Query("SELECT * FROM courses ORDER BY id ASC")
    fun getCoursesWithLessonsStream(): Flow<List<CourseWithLessons>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseWithLessonsStream(courseId: Int): Flow<CourseWithLessons?>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: Int): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Int): LessonEntity?

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    suspend fun getLessonsForCourse(courseId: Int): List<LessonEntity>

    @Query("UPDATE lessons SET isCompleted = :isCompleted WHERE id = :lessonId")
    suspend fun updateLessonCompletion(lessonId: Int, isCompleted: Boolean)

    @Query("UPDATE courses SET progress = :progress WHERE id = :courseId")
    suspend fun updateCourseProgress(courseId: Int, progress: Int)

    @Transaction
    suspend fun toggleLessonAndRecalculateProgress(courseId: Int, lessonId: Int) {
        val lesson = getLessonById(lessonId) ?: return
        val newStatus = !lesson.isCompleted
        updateLessonCompletion(lessonId, newStatus)

        val allLessons = getLessonsForCourse(courseId)
        if (allLessons.isNotEmpty()) {
            val completed = allLessons.count { if (it.id == lessonId) newStatus else it.isCompleted }
            val newProgress = ((completed.toDouble() / allLessons.size.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            updateCourseProgress(courseId, newProgress)
        }
    }

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Query("DELETE FROM lessons")
    suspend fun clearLessons()
}
