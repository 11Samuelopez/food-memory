package com.example.foodmemory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodmemory.data.local.dao.ExperienceDao
import com.example.foodmemory.data.local.dao.RestaurantDao
import com.example.foodmemory.data.local.entity.DishEntity
import com.example.foodmemory.data.local.entity.FoodExperienceEntity
import com.example.foodmemory.data.local.entity.RestaurantEntity
import com.example.foodmemory.data.local.entity.IngredientEntity

@Database(
    entities = [RestaurantEntity::class, FoodExperienceEntity::class, DishEntity::class, IngredientEntity::class],
    version = 2,
    exportSchema = true
)
abstract class FoodMemoryDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
    abstract fun experienceDao(): ExperienceDao

    companion object {
        @Volatile private var instance: FoodMemoryDatabase? = null

        fun getInstance(context: Context): FoodMemoryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FoodMemoryDatabase::class.java,
                "food-memory.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dishes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dishes ADD COLUMN photoPath TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dishId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        evidence TEXT NOT NULL,
                        FOREIGN KEY(dishId) REFERENCES dishes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ingredients_dishId ON ingredients(dishId)")
            }
        }
    }
}
