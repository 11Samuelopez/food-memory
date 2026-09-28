package com.example.foodmemory.data.demo

import com.example.foodmemory.domain.model.DishAnalysis
import com.example.foodmemory.domain.repository.DishAnalysisRepository

/** Demo implementation. Replace with the remote vision adapter when AI is enabled. */
class DemoDishAnalysisRepository : DishAnalysisRepository {
    override suspend fun analyze(photoPath: String) = DishAnalysis(
        isFood = true,
        dishName = "",
        description = "",
        ingredients = emptyList(),
        uncertainty = ""
    )
}
