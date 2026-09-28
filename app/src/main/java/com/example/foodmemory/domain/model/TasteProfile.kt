package com.example.foodmemory.domain.model

data class TasteProfile(
    val favoriteDishes: List<FavoriteDish>,
    val placesToRepeat: List<FavoriteRestaurant>,
    val averageSpendCents: Long?,
    val hasHistory: Boolean
)

data class AiTasteRecommendation(val type: RecommendationType, val name: String, val reason: String)

enum class RecommendationType { DISH, RESTAURANT }

data class FavoriteDish(val name: String, val rating: Double)
data class FavoriteRestaurant(val name: String, val rating: Double)
