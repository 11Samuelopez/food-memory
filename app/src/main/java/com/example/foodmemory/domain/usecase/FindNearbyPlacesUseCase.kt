package com.example.foodmemory.domain.usecase

import com.example.foodmemory.domain.model.NearbyRestaurant
import com.example.foodmemory.domain.repository.NearbyPlacesRepository

class FindNearbyPlacesUseCase(private val repository: NearbyPlacesRepository) {
    suspend operator fun invoke(latitude: Double, longitude: Double): List<NearbyRestaurant> =
        repository.findNearby(latitude, longitude)
}
