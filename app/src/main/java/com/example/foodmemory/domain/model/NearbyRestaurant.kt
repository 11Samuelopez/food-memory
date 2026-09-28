package com.example.foodmemory.domain.model

data class NearbyRestaurant(
    val id: Long,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val distanceMeters: Int
)
