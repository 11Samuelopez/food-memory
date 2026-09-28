package com.example.foodmemory.domain.model

data class FoodExperience(
    val id: Long,
    val restaurantName: String,
    val city: String,
    val visitDate: String,
    val priceCents: Long?,
    val overallRating: Double?,
    val companions: String,
    val notes: String,
    val dishes: List<FoodDish>
)

data class FoodDish(
    val name: String,
    val rating: Double?,
    val description: String,
    val photoPath: String?,
    val ingredients: List<DishIngredient>
)
