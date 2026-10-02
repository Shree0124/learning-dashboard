package com.learning.dashboard.data.repository

import com.learning.dashboard.data.network.NetworkConnectivityObserver
import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.model.User
import com.learning.dashboard.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class AuthRepositoryImpl(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override fun isConnected(): Boolean = connectivityObserver.isConnectedNow()

    override fun observeNetworkConnectivity(): Flow<Boolean> = connectivityObserver.isConnected

    override suspend fun login(email: String, password: String): Resource<User> = withContext(ioDispatcher) {
        if (!connectivityObserver.isConnectedNow()) {
            return@withContext Resource.Error("No internet connection. Please connect to the internet to log in.")
        }

        delay(600L) // Simulate network authentication round-trip

        // Mock authentication check
        if (password == "wrongpassword" || password == "error") {
            return@withContext Resource.Error("Invalid email or password. Please verify your credentials.")
        }

        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = User(
            email = email,
            name = name,
            token = "jwt_mock_${UUID.randomUUID()}"
        )
        _currentUser.value = user
        Resource.Success(user)
    }

    override fun getSessionUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityObserver.isConnectedNow()) {
            return@withContext Result.failure(
                IOException("No internet connection. You cannot log out while offline.")
            )
        }
        _currentUser.value = null
        Result.success(Unit)
    }
}
