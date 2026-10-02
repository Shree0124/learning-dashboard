package com.learning.dashboard.domain.repository

import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    fun getSessionUser(): Flow<User?>
    suspend fun logout()
}
