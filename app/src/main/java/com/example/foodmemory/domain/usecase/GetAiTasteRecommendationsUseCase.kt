package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.AiTasteRecommendation
import com.example.foodmemory.domain.model.TasteProfile
import com.example.foodmemory.domain.repository.TasteRecommendationRepository

/** Keeps the presentation layer independent from Gemini, HTTP, and fallback details. */
class GetAiTasteRecommendationsUseCase(private val repository: TasteRecommendationRepository) {
    suspend operator fun invoke(profile: TasteProfile): List<AiTasteRecommendation> = repository.recommend(profile)
}
