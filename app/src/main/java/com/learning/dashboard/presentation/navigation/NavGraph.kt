package com.learning.dashboard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.learning.dashboard.di.AppContainer
import com.learning.dashboard.presentation.dashboard.DashboardScreen
import com.learning.dashboard.presentation.dashboard.DashboardViewModel
import com.learning.dashboard.presentation.details.CourseDetailScreen
import com.learning.dashboard.presentation.details.CourseDetailViewModel
import com.learning.dashboard.presentation.login.LoginScreen
import com.learning.dashboard.presentation.login.LoginViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    container: AppContainer,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(container.loginUseCase)
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(
                    container.getCoursesUseCase,
                    container.authRepository
                )
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
                onCourseClick = { courseId ->
                    navController.navigate(Screen.CourseDetail.createRoute(courseId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CourseDetail.route,
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 1
            val courseDetailViewModel: CourseDetailViewModel = viewModel(
                factory = CourseDetailViewModel.Factory(
                    courseId = courseId,
                    getCourseDetailUseCase = container.getCourseDetailUseCase,
                    toggleLessonCompletionUseCase = container.toggleLessonCompletionUseCase
                )
            )
            CourseDetailScreen(
                viewModel = courseDetailViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
