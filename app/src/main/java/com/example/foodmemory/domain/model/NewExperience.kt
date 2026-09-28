package com.example.foodmemory.domain.model

data class NewExperience(
    val restaurantName: String,
    val city: String,
    val visitDate: String,
    val priceCents: Long?,
    val overallRating: Double?,
    val companions: String,
    val notes: String,
    val dishes: List<NewDish>
)

data class NewDish(
    val name: String,
    val rating: Double?,
    val description: String = "",
    val photoPath: String? = null,
    val ingredients: List<DishIngredient> = emptyList()
)

data class DishIngredient(val name: String, val evidence: IngredientEvidence)

enum class IngredientEvidence { VISIBLE, TYPICAL }
