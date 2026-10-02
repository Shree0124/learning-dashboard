package com.learning.dashboard.domain.usecase

import com.learning.dashboard.domain.model.Resource
import com.learning.dashboard.domain.model.User
import com.learning.dashboard.domain.repository.AuthRepository
import java.util.regex.Pattern

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    companion object {
        private val EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        const val MIN_PASSWORD_LENGTH = 6
    }

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val emailError: String?, val passwordError: String?) : ValidationResult
    }

    fun validateInput(email: String, password: String): ValidationResult {
        val trimmedEmail = email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> "Email cannot be empty"
            !EMAIL_REGEX.matcher(trimmedEmail).matches() -> "Please enter a valid email address (e.g. user@example.com)"
            else -> null
        }

        val passwordError = when {
            password.isEmpty() -> "Password cannot be empty"
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }

        return if (emailError == null && passwordError == null) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(emailError, passwordError)
        }
    }

    suspend fun execute(email: String, password: String): Resource<User> {
        val validation = validateInput(email, password)
        if (validation is ValidationResult.Invalid) {
            val message = validation.emailError ?: validation.passwordError ?: "Invalid credentials"
            return Resource.Error(message)
        }
        return authRepository.login(email.trim(), password)
    }
}
