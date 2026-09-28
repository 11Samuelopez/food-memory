package com.example.foodmemory.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodmemory.domain.model.UserSession
import com.example.foodmemory.domain.usecase.ClearSessionUseCase
import com.example.foodmemory.domain.usecase.GetSessionUseCase
import com.example.foodmemory.domain.usecase.StartSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SessionError { SAVE, CLEAR }

data class SessionUiState(
    val isLoading: Boolean = true,
    val user: UserSession? = null,
    val error: SessionError? = null,
    val shouldRequestLocationPermission: Boolean = false
)

class SessionViewModel(
    private val getSession: GetSessionUseCase,
    private val startSession: StartSessionUseCase,
    private val clearSession: ClearSessionUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(SessionUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch { mutableState.value = SessionUiState(isLoading = false, user = getSession()) }
    }

    fun signIn(name: String, email: String) {
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { startSession(name, email) }
                .onSuccess {
                    mutableState.value = SessionUiState(
                        isLoading = false,
                        user = it,
                        shouldRequestLocationPermission = true
                    )
                }
                .onFailure { mutableState.value = mutableState.value.copy(isLoading = false, error = SessionError.SAVE) }
        }
    }

    fun signOut() {
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { clearSession() }
                .onSuccess { mutableState.value = SessionUiState(isLoading = false) }
                .onFailure { mutableState.value = mutableState.value.copy(isLoading = false, error = SessionError.CLEAR) }
        }
    }

    fun consumeLocationPermissionRequest() {
        if (mutableState.value.shouldRequestLocationPermission) {
            mutableState.value = mutableState.value.copy(shouldRequestLocationPermission = false)
        }
    }
}
