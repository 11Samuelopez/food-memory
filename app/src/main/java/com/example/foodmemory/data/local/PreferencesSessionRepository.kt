package com.example.foodmemory.data.local

import android.content.Context
import com.example.foodmemory.domain.model.UserSession
import com.example.foodmemory.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesSessionRepository(context: Context) : SessionRepository {
    private val preferences = context.getSharedPreferences("food_memory_session", Context.MODE_PRIVATE)

    override suspend fun getSession(): UserSession? = withContext(Dispatchers.IO) {
        val id = preferences.getString(KEY_USER_ID, null) ?: return@withContext null
        val name = preferences.getString(KEY_NAME, null) ?: return@withContext null
        val email = preferences.getString(KEY_EMAIL, null) ?: return@withContext null
        UserSession(id, name, email)
    }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        check(preferences.edit().putString(KEY_USER_ID, session.userId).putString(KEY_NAME, session.displayName)
            .putString(KEY_EMAIL, session.email).commit())
    }

    override suspend fun clearSession() = withContext(Dispatchers.IO) {
        check(preferences.edit().clear().commit())
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
    }
}
