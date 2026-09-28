package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.repository.ExperienceRepository

class ObserveExperiencesUseCase(private val repository: ExperienceRepository) {
    operator fun invoke() = repository.observeExperiences()
}
