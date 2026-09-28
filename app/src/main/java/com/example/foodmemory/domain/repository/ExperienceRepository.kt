package com.example.foodmemory.domain.repository

import com.example.foodmemory.domain.model.FoodExperience
import com.example.foodmemory.domain.model.NewExperience
import kotlinx.coroutines.flow.Flow

interface ExperienceRepository {
    fun observeExperiences(): Flow<List<FoodExperience>>
    suspend fun addExperience(experience: NewExperience)
}
