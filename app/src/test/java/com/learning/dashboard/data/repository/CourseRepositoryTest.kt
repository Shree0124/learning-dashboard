package com.learning.dashboard.data.repository

import com.learning.dashboard.data.local.dao.CourseDao
import com.learning.dashboard.data.local.entity.CourseEntity
import com.learning.dashboard.data.local.entity.CourseWithLessons
import com.learning.dashboard.data.local.entity.LessonEntity
import com.learning.dashboard.data.network.NetworkConnectivityObserver
import com.learning.dashboard.data.remote.CourseApiService
import com.learning.dashboard.data.remote.dto.CourseDto
import com.learning.dashboard.data.remote.dto.LessonDto
import com.learning.dashboard.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CourseRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeCourseDao
    private lateinit var fakeApiService: FakeCourseApiService
    private lateinit var fakeConnectivityObserver: FakeNetworkConnectivityObserver
    private lateinit var repository: CourseRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeCourseDao()
        fakeApiService = FakeCourseApiService()
        fakeConnectivityObserver = FakeNetworkConnectivityObserver(isConnectedValue = true)
        repository = CourseRepositoryImpl(
            courseDao = fakeDao,
            apiService = fakeApiService,
            connectivityObserver = fakeConnectivityObserver,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `refreshCourses inserts courses and lessons into local DAO when online`() = runTest(testDispatcher) {
        val remoteCourses = listOf(
            CourseDto(
                id = 1,
                title = "Python Programming",
                instructor = "John Smith",
                progress = 50,
                lessons = 2,
                lessonItems = listOf(
                    LessonDto(id = 101, courseId = 1, title = "Intro", isCompleted = true, orderIndex = 1),
                    LessonDto(id = 102, courseId = 1, title = "Variables", isCompleted = false, orderIndex = 2)
                )
            )
        )
        fakeApiService.coursesToReturn = remoteCourses

        val result = repository.refreshCourses()

        assertTrue(result.isSuccess)
        assertEquals(1, fakeDao.insertedCourses.size)
        assertEquals(2, fakeDao.insertedLessons.size)
        assertEquals("Python Programming", fakeDao.insertedCourses[0].title)
        assertEquals(50, fakeDao.insertedCourses[0].progress)
    }

    @Test
    fun `refreshCourses returns failure when device is offline without calling API`() = runTest(testDispatcher) {
        fakeConnectivityObserver.setConnected(false)

        val result = repository.refreshCourses()

        assertTrue(result.isFailure)
        assertEquals(0, fakeApiService.callCount)
    }

    @Test
    fun `getCoursesStream returns cached data when device is offline`() = runTest(testDispatcher) {
        // Pre-populate DAO cache
        val cachedCourse = CourseEntity(1, "Cached Python", "John Smith", 50, 2)
        val cachedLessons = listOf(
            LessonEntity(101, 1, "Intro", true, 1),
            LessonEntity(102, 1, "Variables", false, 2)
        )
        fakeDao.insertCourses(listOf(cachedCourse))
        fakeDao.insertLessons(cachedLessons)

        // Device is offline
        fakeConnectivityObserver.setConnected(false)

        val emissions = repository.getCoursesStream().take(2).toList()

        // First emission is Loading
        assertEquals(Resource.Loading, emissions[0])

        // Second emission serves cached data despite being offline
        assertTrue(emissions[1] is Resource.Success)
        val data = (emissions[1] as Resource.Success).data
        assertEquals(1, data.size)
        assertEquals("Cached Python", data[0].title)
    }

    @Test
    fun `toggleLessonCompletion delegates to DAO transaction`() = runTest(testDispatcher) {
        val result = repository.toggleLessonCompletion(courseId = 1, lessonId = 101)
        assertTrue(result.isSuccess)
        assertEquals(listOf(1 to 101), fakeDao.toggledRecords)
    }

    private class FakeCourseDao : CourseDao {
        val insertedCourses = mutableListOf<CourseEntity>()
        val insertedLessons = mutableListOf<LessonEntity>()
        val toggledRecords = mutableListOf<Pair<Int, Int>>()

        private val coursesFlow = MutableStateFlow<List<CourseWithLessons>>(emptyList())

        override fun getCoursesWithLessonsStream(): Flow<List<CourseWithLessons>> = coursesFlow.asStateFlow()

        override fun getCourseWithLessonsStream(courseId: Int): Flow<CourseWithLessons?> {
            return MutableStateFlow(
                coursesFlow.value.find { it.course.id == courseId }
            )
        }

        override suspend fun getCourseCount(): Int = insertedCourses.size

        override suspend fun getCourseById(courseId: Int): CourseEntity? =
            insertedCourses.find { it.id == courseId }

        override suspend fun insertCourses(courses: List<CourseEntity>) {
            insertedCourses.clear()
            insertedCourses.addAll(courses)
            updateFlow()
        }

        override suspend fun insertLessons(lessons: List<LessonEntity>) {
            insertedLessons.clear()
            insertedLessons.addAll(lessons)
            updateFlow()
        }

        private fun updateFlow() {
            val list = insertedCourses.map { course ->
                CourseWithLessons(
                    course = course,
                    lessons = insertedLessons.filter { it.courseId == course.id }
                )
            }
            coursesFlow.value = list
        }

        override suspend fun getLessonById(lessonId: Int): LessonEntity? =
            insertedLessons.find { it.id == lessonId }

        override suspend fun getLessonsForCourse(courseId: Int): List<LessonEntity> =
            insertedLessons.filter { it.courseId == courseId }

        override suspend fun updateLessonCompletion(lessonId: Int, isCompleted: Boolean) {
            val index = insertedLessons.indexOfFirst { it.id == lessonId }
            if (index != -1) {
                insertedLessons[index] = insertedLessons[index].copy(isCompleted = isCompleted)
                updateFlow()
            }
        }

        override suspend fun updateCourseProgress(courseId: Int, progress: Int) {
            val index = insertedCourses.indexOfFirst { it.id == courseId }
            if (index != -1) {
                insertedCourses[index] = insertedCourses[index].copy(progress = progress)
                updateFlow()
            }
        }

        override suspend fun toggleLessonAndRecalculateProgress(courseId: Int, lessonId: Int) {
            toggledRecords.add(courseId to lessonId)
        }

        override suspend fun clearCourses() {
            insertedCourses.clear()
            updateFlow()
        }

        override suspend fun clearLessons() {
            insertedLessons.clear()
            updateFlow()
        }
    }

    private class FakeCourseApiService : CourseApiService {
        override var forceSimulateError: Boolean = false
        override var isSimulatedOffline: Boolean = false
        var shouldThrowError = false
        var coursesToReturn = listOf<CourseDto>()
        var callCount = 0

        override suspend fun getCourses(): List<CourseDto> {
            callCount++
            if (shouldThrowError) throw IOException("Simulated network outage")
            return coursesToReturn
        }

        override suspend fun getCourseDetails(courseId: Int): CourseDto? {
            callCount++
            if (shouldThrowError) throw IOException("Simulated network outage")
            return coursesToReturn.find { it.id == courseId }
        }
    }

    private class FakeNetworkConnectivityObserver(
        var isConnectedValue: Boolean = true
    ) : NetworkConnectivityObserver {
        private val _isConnected = MutableStateFlow(isConnectedValue)
        override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
        override fun isConnectedNow(): Boolean = isConnectedValue

        fun setConnected(connected: Boolean) {
            isConnectedValue = connected
            _isConnected.value = connected
        }
    }
}
