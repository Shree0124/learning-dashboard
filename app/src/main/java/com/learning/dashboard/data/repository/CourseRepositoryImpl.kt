package com.learning.dashboard.data.repository

import com.learning.dashboard.data.local.dao.CourseDao
import com.learning.dashboard.data.local.entity.CourseEntity
import com.learning.dashboard.data.local.entity.LessonEntity
import com.learning.dashboard.data.network.NetworkConnectivityObserver
import com.learning.dashboard.data.remote.CourseApiService
import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.repository.CourseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class CourseRepositoryImpl(
    private val courseDao: CourseDao,
    private val apiService: CourseApiService,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CourseRepository {

    override fun observeNetworkConnectivity(): Flow<Boolean> = connectivityObserver.isConnected

    override fun isConnected(): Boolean = connectivityObserver.isConnectedNow()

    override fun getCoursesStream(): Flow<Resource<List<Course>>> = flow {
        emit(Resource.Loading)

        // Read initial cached courses from local Room database
        val cached = withContext(ioDispatcher) {
            courseDao.getCoursesWithLessonsStream().firstOrNull()?.map { it.toDomain() }
        }

        if (!cached.isNullOrEmpty()) {
            emit(Resource.Success(cached))
        }

        // Attempt remote refresh only if device is connected to the internet
        val refreshResult = if (connectivityObserver.isConnectedNow()) {
            refreshCourses()
        } else {
            Result.failure(IOException("No internet connection."))
        }

        if (refreshResult.isFailure && cached.isNullOrEmpty()) {
            val errorMsg = if (!connectivityObserver.isConnectedNow()) {
                "No internet connection. Please connect to the internet to load courses."
            } else {
                refreshResult.exceptionOrNull()?.message ?: "Failed to load courses from network."
            }
            emit(Resource.Error(errorMsg, refreshResult.exceptionOrNull()))
            return@flow
        }

        // Continuously observe Room database as single source of truth
        courseDao.getCoursesWithLessonsStream()
            .map { list ->
                val domainList = list.map { it.toDomain() }
                if (domainList.isEmpty() && refreshResult.isFailure) {
                    Resource.Error(refreshResult.exceptionOrNull()?.message ?: "No courses available.")
                } else {
                    Resource.Success(domainList)
                }
            }
            .catch { e ->
                emit(Resource.Error("Local database error: ${e.message}", e))
            }
            .collect { emit(it) }
    }

    override suspend fun refreshCourses(): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityObserver.isConnectedNow()) {
            return@withContext Result.failure(IOException("No internet connection."))
        }

        try {
            val remoteCourses = apiService.getCourses()

            // Map and persist to local Room database
            val courseEntities = mutableListOf<CourseEntity>()
            val lessonEntities = mutableListOf<LessonEntity>()

            for (dto in remoteCourses) {
                // If lessons already exist locally, preserve user completion state
                val existingLessons = courseDao.getLessonsForCourse(dto.id)
                val existingCompletionMap = existingLessons.associate { it.id to it.isCompleted }

                courseEntities.add(
                    CourseEntity(
                        id = dto.id,
                        title = dto.title,
                        instructor = dto.instructor,
                        progress = dto.progress,
                        lessonsCount = dto.lessons
                    )
                )

                for (lessonDto in dto.lessonItems) {
                    val isCompleted = existingCompletionMap[lessonDto.id] ?: lessonDto.isCompleted
                    lessonEntities.add(
                        LessonEntity(
                            id = lessonDto.id,
                            courseId = lessonDto.courseId,
                            title = lessonDto.title,
                            isCompleted = isCompleted,
                            orderIndex = lessonDto.orderIndex
                        )
                    )
                }
            }

            courseDao.insertCourses(courseEntities)
            if (lessonEntities.isNotEmpty()) {
                courseDao.insertLessons(lessonEntities)
            }

            // Sync progress after preserving lessons
            for (c in courseEntities) {
                val lessons = courseDao.getLessonsForCourse(c.id)
                if (lessons.isNotEmpty()) {
                    val completed = lessons.count { it.isCompleted }
                    val progress = ((completed.toDouble() / lessons.size.toDouble()) * 100.0).toInt().coerceIn(0, 100)
                    courseDao.updateCourseProgress(c.id, progress)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCourseStream(courseId: Int): Flow<Course?> {
        return courseDao.getCourseWithLessonsStream(courseId)
            .map { it?.toDomain() }
    }

    override suspend fun toggleLessonCompletion(courseId: Int, lessonId: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            courseDao.toggleLessonAndRecalculateProgress(courseId, lessonId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
