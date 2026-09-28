package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.DishAnalysis
import com.example.foodmemory.domain.repository.DishAnalysisRepository

class AnalyzeDishPhotoUseCase(private val repository: DishAnalysisRepository) {
    suspend operator fun invoke(photoPath: String): DishAnalysis = repository.analyze(photoPath)
}
