package com.example.foodmemory.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.foodmemory.data.local.entity.DishEntity
import com.example.foodmemory.data.local.entity.IngredientEntity
import com.example.foodmemory.data.local.entity.FoodExperienceEntity
import com.example.foodmemory.data.local.entity.RestaurantEntity

data class ExperienceWithDetails(
    @Embedded val experience: FoodExperienceEntity,
    @Relation(parentColumn = "restaurantId", entityColumn = "id")
    val restaurant: RestaurantEntity,
    @Relation(parentColumn = "id", entityColumn = "experienceId", entity = DishEntity::class)
    val dishes: List<DishWithIngredients>
)

data class DishWithIngredients(
    @Embedded val dish: DishEntity,
    @Relation(parentColumn = "id", entityColumn = "dishId")
    val ingredients: List<IngredientEntity>
)
