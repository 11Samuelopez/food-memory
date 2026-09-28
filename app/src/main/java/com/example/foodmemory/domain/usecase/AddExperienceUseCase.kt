package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.NewExperience
import com.example.foodmemory.domain.repository.ExperienceRepository

class AddExperienceUseCase(private val repository: ExperienceRepository) {
    suspend operator fun invoke(experience: NewExperience) = repository.addExperience(experience)
}
