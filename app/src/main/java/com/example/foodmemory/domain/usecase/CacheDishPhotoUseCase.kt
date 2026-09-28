package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.repository.DishPhotoRepository

class CacheDishPhotoUseCase(private val repository: DishPhotoRepository) {
    suspend operator fun invoke(photoPath: String): String = repository.cache(photoPath)
}
