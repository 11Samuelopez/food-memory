package com.example.foodmemory.data.demo

import com.example.foodmemory.domain.model.AiTasteRecommendation
import com.example.foodmemory.domain.model.RecommendationType
import com.example.foodmemory.domain.model.TasteProfile
import com.example.foodmemory.domain.repository.TasteRecommendationRepository

/** Deterministic offline fallback: every suggestion is directly traceable to a saved rating. */
class DemoTasteRecommendationRepository : TasteRecommendationRepository {
    override suspend fun recommend(profile: TasteProfile): List<AiTasteRecommendation> = buildList {
        profile.favoriteDishes.take(3).forEach { dish ->
            add(AiTasteRecommendation(RecommendationType.DISH, dish.name, "Te gustó mucho (%.1f/5).".format(dish.rating)))
        }
        profile.placesToRepeat.take(2).forEach { place ->
            add(AiTasteRecommendation(RecommendationType.RESTAURANT, place.name, "Valoraste tu experiencia con %.1f/5.".format(place.rating)))
        }
    }
}
