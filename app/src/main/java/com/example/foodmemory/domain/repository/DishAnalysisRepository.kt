package com.example.foodmemory.domain.repository

import com.example.foodmemory.domain.model.DishAnalysis
interface DishAnalysisRepository {
    suspend fun analyze(photoPath: String): DishAnalysis
}
