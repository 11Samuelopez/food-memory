package com.example.foodmemory.domain.model

data class DishAnalysis(
    val isFood: Boolean,
    val dishName: String,
    val description: String,
    val ingredients: List<DishIngredient>,
    val uncertainty: String
)
