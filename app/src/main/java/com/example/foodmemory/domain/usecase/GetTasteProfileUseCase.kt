package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.FavoriteDish
import com.example.foodmemory.domain.model.FavoriteRestaurant
import com.example.foodmemory.domain.model.FoodExperience
import com.example.foodmemory.domain.model.TasteProfile

class GetTasteProfileUseCase {
    operator fun invoke(experiences: List<FoodExperience>): TasteProfile {
        val favoriteDishes = experiences.asSequence()
            .flatMap { it.dishes.asSequence() }
            .mapNotNull { dish -> dish.rating?.takeIf { it >= 4.0 }?.let { FavoriteDish(dish.name, it) } }
            .distinctBy { it.name.lowercase() }
            .sortedByDescending { it.rating }
            .take(3)
            .toList()

        val placesToRepeat = experiences.asSequence()
            .mapNotNull { visit -> visit.overallRating?.takeIf { it >= 4.0 }?.let { visit.restaurantName to it } }
            .groupBy({ it.first }, { it.second })
            .map { (name, ratings) -> FavoriteRestaurant(name, ratings.average()) }
            .sortedByDescending { it.rating }
            .take(3)

        val prices = experiences.mapNotNull { it.priceCents }
        val averageSpend = prices.takeIf { it.isNotEmpty() }?.average()?.toLong()

        return TasteProfile(
            favoriteDishes = favoriteDishes,
            placesToRepeat = placesToRepeat,
            averageSpendCents = averageSpend,
            hasHistory = experiences.isNotEmpty()
        )
    }
}
