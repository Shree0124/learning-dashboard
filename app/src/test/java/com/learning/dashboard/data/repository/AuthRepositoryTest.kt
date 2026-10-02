package com.learning.dashboard.data.repository

import com.learning.dashboard.data.network.NetworkConnectivityObserver
import com.learning.dashboard.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeConnectivityObserver: FakeConnectivityObserver
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeConnectivityObserver = FakeConnectivityObserver(isConnectedState = true)
        authRepository = AuthRepositoryImpl(
            connectivityObserver = fakeConnectivityObserver,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `login with valid credentials succeeds and sets current user`() = runTest(testDispatcher) {
        val result = authRepository.login("john.doe@example.com", "password123")

        assertTrue(result is Resource.Success)
        val user = (result as Resource.Success).data
        assertEquals("john.doe@example.com", user.email)
        assertEquals("John.doe", user.name)
        assertNotNull(user.token)
    }

    @Test
    fun `login with incorrect password returns invalid credentials error`() = runTest(testDispatcher) {
        val result = authRepository.login("john.doe@example.com", "wrongpassword999")

        assertTrue(result is Resource.Error)
        val errorMsg = (result as Resource.Error).message
        assertEquals("Invalid email or password. Please verify your credentials.", errorMsg)
    }

    @Test
    fun `login with unregistered email returns invalid credentials error`() = runTest(testDispatcher) {
        val result = authRepository.login("unknown.stranger@example.com", "password123")

        assertTrue(result is Resource.Error)
        val errorMsg = (result as Resource.Error).message
        assertEquals("Invalid email or password. Please verify your credentials.", errorMsg)
    }

    @Test
    fun `login while offline returns no internet error message`() = runTest(testDispatcher) {
        fakeConnectivityObserver.setConnected(false)

        val result = authRepository.login("john.doe@example.com", "password123")

        assertTrue(result is Resource.Error)
        val errorMsg = (result as Resource.Error).message
        assertTrue(errorMsg.contains("internet", ignoreCase = true))
    }

    @Test
    fun `logout while online clears session user`() = runTest(testDispatcher) {
        authRepository.login("john.doe@example.com", "password123")

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)
    }

    @Test
    fun `logout while offline returns failure`() = runTest(testDispatcher) {
        authRepository.login("john.doe@example.com", "password123")
        fakeConnectivityObserver.setConnected(false)

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isFailure)
        assertTrue(logoutResult.exceptionOrNull()?.message?.contains("offline", ignoreCase = true) == true)
    }

    private class FakeConnectivityObserver(
        var isConnectedState: Boolean = true
    ) : NetworkConnectivityObserver {
        private val _isConnected = MutableStateFlow(isConnectedState)
        override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
        override fun isConnectedNow(): Boolean = isConnectedState

        fun setConnected(connected: Boolean) {
            isConnectedState = connected
            _isConnected.value = connected
        }
    }
}
