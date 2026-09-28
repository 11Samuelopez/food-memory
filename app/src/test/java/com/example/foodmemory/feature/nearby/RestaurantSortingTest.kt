package com.example.foodmemory.feature.nearby

import com.example.foodmemory.domain.model.NearbyRestaurant
import org.junit.Assert.assertEquals
import org.junit.Test

class RestaurantSortingTest {
    @Test
    fun spanishAccentsSortWithTheirBaseLetter() {
        val places = listOf(
            restaurant(1, "Zara"),
            restaurant(2, "Ágora"),
            restaurant(3, "Arenal")
        )

        val ascending = sortBySpanishName(places, SortDirection.ASCENDING)
        val descending = sortBySpanishName(places, SortDirection.DESCENDING)

        assertEquals(listOf("Ágora", "Arenal", "Zara"), ascending.map { it.name })
        assertEquals(listOf("Zara", "Arenal", "Ágora"), descending.map { it.name })
    }

    private fun restaurant(id: Long, name: String) = NearbyRestaurant(
        id = id,
        name = name,
        category = "restaurant",
        latitude = 37.39,
        longitude = -5.99,
        address = null,
        distanceMeters = id.toInt()
    )
}
