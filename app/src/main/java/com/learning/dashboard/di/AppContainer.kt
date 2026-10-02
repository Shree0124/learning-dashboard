package com.learning.dashboard.di

import android.content.Context
import com.learning.dashboard.data.local.AppDatabase
import com.learning.dashboard.data.local.dao.CourseDao
import com.learning.dashboard.data.remote.CourseApiService
import com.learning.dashboard.data.remote.MockCourseApiService
import com.learning.dashboard.data.repository.AuthRepositoryImpl
import com.learning.dashboard.data.repository.CourseRepositoryImpl
import com.learning.dashboard.domain.repository.AuthRepository
import com.learning.dashboard.domain.repository.CourseRepository
import com.learning.dashboard.domain.usecase.CalculateProgressUseCase
import com.learning.dashboard.domain.usecase.GetCourseDetailUseCase
import com.learning.dashboard.domain.usecase.GetCoursesUseCase
import com.learning.dashboard.domain.usecase.LoginUseCase
import com.learning.dashboard.domain.usecase.ToggleLessonCompletionUseCase
import kotlinx.coroutines.Dispatchers

interface AppContainer {
    val database: AppDatabase
    val courseDao: CourseDao
    val apiService: CourseApiService
    val courseRepository: CourseRepository
    val authRepository: AuthRepository

    val calculateProgressUseCase: CalculateProgressUseCase
    val getCoursesUseCase: GetCoursesUseCase
    val getCourseDetailUseCase: GetCourseDetailUseCase
    val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
    val loginUseCase: LoginUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val courseDao: CourseDao by lazy {
        database.courseDao()
    }

    override val apiService: CourseApiService by lazy {
        MockCourseApiService()
    }

    override val courseRepository: CourseRepository by lazy {
        CourseRepositoryImpl(
            courseDao = courseDao,
            apiService = apiService,
            ioDispatcher = Dispatchers.IO
        )
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            ioDispatcher = Dispatchers.IO
        )
    }

    override val calculateProgressUseCase: CalculateProgressUseCase by lazy {
        CalculateProgressUseCase()
    }

    override val getCoursesUseCase: GetCoursesUseCase by lazy {
        GetCoursesUseCase(courseRepository)
    }

    override val getCourseDetailUseCase: GetCourseDetailUseCase by lazy {
        GetCourseDetailUseCase(courseRepository)
    }

    override val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase by lazy {
        ToggleLessonCompletionUseCase(courseRepository)
    }

    override val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }
}
