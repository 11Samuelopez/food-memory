package com.example.foodmemory.domain.repository

import com.example.foodmemory.domain.model.NearbyRestaurant

interface NearbyPlacesRepository {
    suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyRestaurant>
}
