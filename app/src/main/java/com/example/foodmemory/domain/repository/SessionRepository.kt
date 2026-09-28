package com.example.foodmemory.domain.repository

import com.example.foodmemory.domain.model.UserSession

interface SessionRepository {
    suspend fun getSession(): UserSession?
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()
}
