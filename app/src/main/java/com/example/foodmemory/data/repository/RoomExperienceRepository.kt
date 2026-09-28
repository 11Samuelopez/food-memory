package com.example.foodmemory.data.repository

import androidx.room.withTransaction
import com.example.foodmemory.data.local.FoodMemoryDatabase
import com.example.foodmemory.data.local.entity.DishEntity
import com.example.foodmemory.data.local.entity.FoodExperienceEntity
import com.example.foodmemory.data.local.entity.IngredientEntity
import com.example.foodmemory.data.local.entity.RestaurantEntity
import com.example.foodmemory.data.local.relation.ExperienceWithDetails
import com.example.foodmemory.domain.model.FoodDish
import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.domain.model.IngredientEvidence
import com.example.foodmemory.domain.model.FoodExperience
import com.example.foodmemory.domain.model.NewExperience
import com.example.foodmemory.domain.repository.ExperienceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExperienceRepository(private val database: FoodMemoryDatabase) : ExperienceRepository {
    override fun observeExperiences(): Flow<List<FoodExperience>> =
        database.experienceDao().observeAllWithDetails().map { entries -> entries.map(::toDomain) }

    private fun toDomain(entry: ExperienceWithDetails) = FoodExperience(
        id = entry.experience.id,
        restaurantName = entry.restaurant.name,
        city = entry.restaurant.city,
        visitDate = entry.experience.visitDate,
        priceCents = entry.experience.priceCents,
        overallRating = entry.experience.overallRating,
        companions = entry.experience.companions,
        notes = entry.experience.notes,
        dishes = entry.dishes.map { dish ->
            FoodDish(
                name = dish.dish.name,
                rating = dish.dish.rating,
                description = dish.dish.description,
                photoPath = dish.dish.photoPath,
                ingredients = dish.ingredients.map {
                    DishIngredient(it.name, IngredientEvidence.valueOf(it.evidence))
                }
            )
        }
    )

    override suspend fun addExperience(experience: NewExperience) {
        database.withTransaction {
            val restaurantDao = database.restaurantDao()
            val name = experience.restaurantName.trim()
            val city = experience.city.trim()
            val existingRestaurant = restaurantDao.findByNameAndCity(name, city)
            val restaurantId = existingRestaurant?.id
                ?: restaurantDao.insert(RestaurantEntity(name = name, city = city))

            val experienceId = database.experienceDao().insertExperience(
                FoodExperienceEntity(
                    restaurantId = restaurantId,
                    visitDate = experience.visitDate,
                    priceCents = experience.priceCents,
                    overallRating = experience.overallRating,
                    companions = experience.companions.trim(),
                    notes = experience.notes.trim()
                )
            )
            experience.dishes
                .filter { it.name.isNotBlank() }
                .forEach { dish ->
                    val dishId = database.experienceDao().insertDish(
                        DishEntity(
                            experienceId = experienceId,
                            name = dish.name.trim(),
                            rating = dish.rating,
                            description = dish.description.trim(),
                            photoPath = dish.photoPath
                        )
                    )
                    val ingredients = dish.ingredients
                        .filter { it.name.isNotBlank() }
                        .map { ingredient ->
                            IngredientEntity(
                                dishId = dishId,
                                name = ingredient.name.trim(),
                                evidence = ingredient.evidence.name
                            )
                        }
                    if (ingredients.isNotEmpty()) database.experienceDao().insertIngredients(ingredients)
                }
        }
    }
}
