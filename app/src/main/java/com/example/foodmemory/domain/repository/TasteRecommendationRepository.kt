package com.example.foodmemory.domain.repository

import com.example.foodmemory.domain.model.AiTasteRecommendation
import com.example.foodmemory.domain.model.TasteProfile

/** Domain boundary for recommendations; implementations may call AI or provide an offline fallback. */
interface TasteRecommendationRepository {
    suspend fun recommend(profile: TasteProfile): List<AiTasteRecommendation>
}
