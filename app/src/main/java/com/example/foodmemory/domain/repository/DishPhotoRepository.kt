package com.example.foodmemory.domain.repository

interface DishPhotoRepository {
    suspend fun cache(sourcePath: String): String
}
