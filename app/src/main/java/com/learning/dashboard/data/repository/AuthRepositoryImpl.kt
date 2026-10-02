package com.learning.dashboard.data.repository

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
import java.util.UUID

class AuthRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override suspend fun login(email: String, password: String): Resource<User> = withContext(ioDispatcher) {
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

    override suspend fun logout() {
        _currentUser.value = null
    }
}
