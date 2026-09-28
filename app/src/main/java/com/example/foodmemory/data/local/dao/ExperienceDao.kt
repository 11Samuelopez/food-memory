package com.example.foodmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.foodmemory.data.local.entity.DishEntity
import com.example.foodmemory.data.local.entity.FoodExperienceEntity
import com.example.foodmemory.data.local.entity.IngredientEntity
import com.example.foodmemory.data.local.relation.ExperienceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceDao {
    @Transaction
    @Query("SELECT * FROM food_experiences ORDER BY visitDate DESC, id DESC")
    fun observeAllWithDetails(): Flow<List<ExperienceWithDetails>>

    @Insert
    suspend fun insertExperience(experience: FoodExperienceEntity): Long

    @Insert
    suspend fun insertDish(dish: DishEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)
}
