package com.example.foodmemory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dishes",
    foreignKeys = [
        ForeignKey(
            entity = FoodExperienceEntity::class,
            parentColumns = ["id"],
            childColumns = ["experienceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("experienceId")]
)
data class DishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experienceId: Long,
    val name: String,
    val rating: Double?,
    val description: String = "",
    val photoPath: String? = null
)
