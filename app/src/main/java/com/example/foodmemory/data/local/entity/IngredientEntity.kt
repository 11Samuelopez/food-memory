package com.example.foodmemory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = DishEntity::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dishId")]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dishId: Long,
    val name: String,
    /** VISIBLE when the photo supports it; TYPICAL when inferred from the dish. */
    val evidence: String
)
