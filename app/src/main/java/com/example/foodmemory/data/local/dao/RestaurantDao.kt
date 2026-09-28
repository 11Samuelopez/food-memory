package com.example.foodmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodmemory.data.local.entity.RestaurantEntity

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM restaurants WHERE name = :name AND city = :city LIMIT 1")
    suspend fun findByNameAndCity(name: String, city: String): RestaurantEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(restaurant: RestaurantEntity): Long
}
