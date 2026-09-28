package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.UserSession
import com.example.foodmemory.domain.repository.SessionRepository
import java.util.UUID

class GetSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.getSession()
}

class SaveSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(session: UserSession) = repository.saveSession(session)
}

class StartSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(name: String, email: String): UserSession {
        val session = UserSession(
            userId = UUID.randomUUID().toString(),
            displayName = name.trim(),
            email = email.trim().lowercase()
        )
        repository.saveSession(session)
        return session
    }
}

class ClearSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.clearSession()
}
