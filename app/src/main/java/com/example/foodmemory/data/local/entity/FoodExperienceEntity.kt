package com.example.foodmemory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_experiences",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("restaurantId"), Index("visitDate")]
)
data class FoodExperienceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: Long,
    /** ISO-8601 local date (yyyy-MM-dd), which sorts chronologically as text. */
    val visitDate: String,
    /** Stored in euro cents to avoid floating-point money values. */
    val priceCents: Long?,
    val overallRating: Double?,
    val companions: String,
    val notes: String
)
