package com.example.foodmemory.feature.dish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodmemory.domain.model.DishAnalysis
import com.example.foodmemory.domain.usecase.AnalyzeDishPhotoUseCase
import com.example.foodmemory.domain.usecase.CacheDishPhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class DishCaptureUiState(
    val analysis: DishAnalysis? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

class DishCaptureViewModel(
    private val analyzeDishPhoto: AnalyzeDishPhotoUseCase,
    private val cacheDishPhoto: CacheDishPhotoUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(DishCaptureUiState())
    val state = mutableState.asStateFlow()

    fun analyze(photoPath: String) = viewModelScope.launch {
        mutableState.update { it.copy(isAnalyzing = true, error = null) }
        try {
            val analysis = analyzeDishPhoto(photoPath)
            mutableState.update { it.copy(analysis = analysis, isAnalyzing = false) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            mutableState.update { it.copy(isAnalyzing = false, error = exception.message) }
        }
    }

    fun clearAnalysis() = mutableState.update { DishCaptureUiState() }

    suspend fun cachePhoto(photoPath: String): String = cacheDishPhoto(photoPath)
}
