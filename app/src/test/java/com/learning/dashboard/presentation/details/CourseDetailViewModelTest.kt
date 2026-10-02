package com.learning.dashboard.presentation.details

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Lesson
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.repository.CourseRepository
import com.learning.dashboard.domain.usecase.GetCourseDetailUseCase
import com.learning.dashboard.domain.usecase.ToggleLessonCompletionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeCourseRepository
    private lateinit var getCourseDetailUseCase: GetCourseDetailUseCase
    private lateinit var toggleLessonUseCase: ToggleLessonCompletionUseCase
    private lateinit var viewModel: CourseDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCourseRepository()
        getCourseDetailUseCase = GetCourseDetailUseCase(fakeRepository)
        toggleLessonUseCase = ToggleLessonCompletionUseCase(fakeRepository)
        viewModel = CourseDetailViewModel(
            courseId = 1,
            getCourseDetailUseCase = getCourseDetailUseCase,
            toggleLessonCompletionUseCase = toggleLessonUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits success with course and lessons`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailUiState.Success)
        val course = (state as CourseDetailUiState.Success).course
        assertEquals(1, course.id)
        assertEquals("Python Programming", course.title)
        assertEquals(2, course.lessons.size)
        assertEquals(50, course.progress)
    }

    @Test
    fun `toggleLesson invokes repository and updates state`() = runTest {
        advanceUntilIdle()

        // Toggle lesson 102 (was false, will become true)
        viewModel.toggleLesson(102)
        advanceUntilIdle()

        assertEquals(listOf(1 to 102), fakeRepository.toggledCalls)

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailUiState.Success)
        val course = (state as CourseDetailUiState.Success).course
        // Both lessons are now completed -> 100% progress
        assertEquals(100, course.progress)
        assertTrue(course.lessons.first { it.id == 102 }.isCompleted)
    }

    private class FakeCourseRepository : CourseRepository {
        val toggledCalls = mutableListOf<Pair<Int, Int>>()

        private val courseFlow = MutableStateFlow<Course?>(
            Course(
                id = 1,
                title = "Python Programming",
                instructor = "John Smith",
                progress = 50,
                lessonsCount = 2,
                lessons = listOf(
                    Lesson(id = 101, courseId = 1, title = "Intro", isCompleted = true, orderIndex = 1),
                    Lesson(id = 102, courseId = 1, title = "Variables", isCompleted = false, orderIndex = 2)
                )
            )
        )

        override fun getCourseStream(courseId: Int): Flow<Course?> = courseFlow.asStateFlow()

        override suspend fun toggleLessonCompletion(courseId: Int, lessonId: Int): Result<Unit> {
            toggledCalls.add(courseId to lessonId)
            val current = courseFlow.value ?: return Result.failure(Exception("Not found"))
            val updatedLessons = current.lessons.map {
                if (it.id == lessonId) it.copy(isCompleted = !it.isCompleted) else it
            }
            val completed = updatedLessons.count { it.isCompleted }
            val newProgress = ((completed.toDouble() / updatedLessons.size.toDouble()) * 100).toInt()
            courseFlow.value = current.copy(
                lessons = updatedLessons,
                progress = newProgress
            )
            return Result.success(Unit)
        }

        override fun getCoursesStream(): Flow<Resource<List<Course>>> = flowOf(Resource.Success(emptyList()))
        override suspend fun refreshCourses(): Result<Unit> = Result.success(Unit)
        override fun observeNetworkConnectivity(): Flow<Boolean> = flowOf(true)
        override fun isConnected(): Boolean = true
    }
}
