package com.example.foodmemory.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodmemory.domain.model.AiTasteRecommendation
import com.example.foodmemory.domain.model.TasteProfile
import com.example.foodmemory.domain.usecase.GetAiTasteRecommendationsUseCase
import com.example.foodmemory.domain.usecase.GetTasteProfileUseCase
import com.example.foodmemory.domain.usecase.ObserveExperiencesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ProfileUiState(
    val tasteProfile: TasteProfile = TasteProfile(emptyList(), emptyList(), null, false),
    val aiRecommendations: List<AiTasteRecommendation> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val hasRecommendationError: Boolean = false
)

/** Publishes grounded local ideas immediately, then replaces them with validated server AI output. */
class ProfileViewModel(
    observeExperiences: ObserveExperiencesUseCase,
    getTasteProfile: GetTasteProfileUseCase,
    private val getAiTasteRecommendations: GetAiTasteRecommendationsUseCase,
    private val getLocalTasteRecommendations: GetAiTasteRecommendationsUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeExperiences().map(getTasteProfile::invoke).collectLatest { profile ->
                mutableState.value = mutableState.value.copy(
                    tasteProfile = profile,
                    aiRecommendations = emptyList(),
                    isLoadingRecommendations = profile.hasHistory,
                    hasRecommendationError = false
                )
                if (profile.hasHistory) {
                    // Fast, deterministic fallback keeps the screen useful while the AI service responds.
                    val localSuggestions = getLocalTasteRecommendations(profile)
                    mutableState.value = mutableState.value.copy(aiRecommendations = localSuggestions)
                    try {
                        val suggestions = getAiTasteRecommendations(profile)
                        mutableState.value = mutableState.value.copy(
                            aiRecommendations = suggestions.ifEmpty { localSuggestions },
                            isLoadingRecommendations = false,
                            hasRecommendationError = suggestions.isEmpty()
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        mutableState.value = mutableState.value.copy(
                            isLoadingRecommendations = false,
                            hasRecommendationError = true
                        )
                    }
                }
            }
        }
    }
}
