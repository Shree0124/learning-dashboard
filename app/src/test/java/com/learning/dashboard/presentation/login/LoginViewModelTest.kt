package com.learning.dashboard.presentation.login

import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.model.User
import com.learning.dashboard.domain.repository.AuthRepository
import com.learning.dashboard.domain.usecase.LoginUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeAuthRepository)
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty email triggers validation error without calling repository`() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("validPassword123")

        viewModel.login()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertEquals("Email cannot be empty", state.emailError)
        assertFalse(state.isLoading)
        assertFalse(state.isLoginSuccessful)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `invalid email format triggers validation error`() = runTest {
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("password123")

        viewModel.login()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertTrue(state.emailError!!.contains("valid email"))
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `password shorter than 6 characters triggers validation error`() = runTest {
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("12345")

        viewModel.login()

        val state = viewModel.uiState.value
        assertNotNull(state.passwordError)
        assertEquals("Password must be at least 6 characters", state.passwordError)
        assertEquals(0, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `valid credentials triggers loading and successful login`() = runTest {
        viewModel.onEmailChanged("student@example.com")
        viewModel.onPasswordChanged("securePassword123")

        viewModel.login()

        // Before dispatching async work, loading is set
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.isLoginSuccessful)
        assertNull(finalState.generalError)
        assertEquals(1, fakeAuthRepository.loginCallCount)
    }

    @Test
    fun `failed login triggers error state with general error message`() = runTest {
        fakeAuthRepository.shouldFail = true
        viewModel.onEmailChanged("student@example.com")
        viewModel.onPasswordChanged("wrongpassword")

        viewModel.login()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertFalse(finalState.isLoginSuccessful)
        assertNotNull(finalState.generalError)
        assertEquals("Invalid credentials provided", finalState.generalError)
    }

    private class FakeAuthRepository : AuthRepository {
        var shouldFail: Boolean = false
        var loginCallCount = 0

        override suspend fun login(email: String, password: String): Resource<User> {
            loginCallCount++
            return if (shouldFail) {
                Resource.Error("Invalid credentials provided")
            } else {
                Resource.Success(User(email = email, name = "Student", token = "mock_token"))
            }
        }

        override fun getSessionUser(): Flow<User?> = flowOf(null)
        override suspend fun logout() {}
    }
}
