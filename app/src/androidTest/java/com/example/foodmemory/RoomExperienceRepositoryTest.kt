package com.example.foodmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.foodmemory.data.local.FoodMemoryDatabase
import com.example.foodmemory.data.local.InternalDishPhotoRepository
import com.example.foodmemory.data.repository.RoomExperienceRepository
import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.domain.model.IngredientEvidence
import com.example.foodmemory.domain.model.NewDish
import com.example.foodmemory.domain.model.NewExperience
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RoomExperienceRepositoryTest {
    private lateinit var database: FoodMemoryDatabase
    private lateinit var repository: RoomExperienceRepository
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
        database = openDatabase()
        repository = RoomExperienceRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun experienceAndRelationsSurviveDatabaseReopen() = runBlocking {
        repository.addExperience(
            NewExperience(
                restaurantName = "  La Brunilda ",
                city = " Sevilla ",
                visitDate = "2026-09-27",
                priceCents = 3250,
                overallRating = 4.5,
                companions = "Marta",
                notes = "Volver por el postre",
                dishes = listOf(
                    NewDish(
                        name = "  Tiramisú ",
                        rating = 5.0,
                        description = "Café y cacao",
                        ingredients = listOf(DishIngredient("Mascarpone", IngredientEvidence.VISIBLE))
                    )
                )
            )
        )

        database.close()
        database = openDatabase()
        repository = RoomExperienceRepository(database)

        val saved = repository.observeExperiences().first().single()
        assertEquals("La Brunilda", saved.restaurantName)
        assertEquals("Sevilla", saved.city)
        assertEquals(3250L, saved.priceCents)
        assertEquals("Tiramisú", saved.dishes.single().name)
        assertEquals("Café y cacao", saved.dishes.single().description)
        assertEquals("Mascarpone", saved.dishes.single().ingredients.single().name)
        assertEquals(IngredientEvidence.VISIBLE, saved.dishes.single().ingredients.single().evidence)
        assertNotNull(saved.id)
    }

    @Test
    fun photoCacheReturnsACompleteDurableCopy() = runBlocking {
        val source = File(context.cacheDir, "test-dish.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val cachedPath = InternalDishPhotoRepository(context).cache(source.absolutePath)

        val cachedFile = File(cachedPath)
        assertEquals(true, cachedFile.isFile)
        assertEquals(source.readBytes().toList(), cachedFile.readBytes().toList())
        assertEquals("dish-photos", cachedFile.parentFile?.name)
    }

    private fun openDatabase(): FoodMemoryDatabase =
        Room.databaseBuilder(context, FoodMemoryDatabase::class.java, TEST_DATABASE_NAME).build()

    companion object {
        private const val TEST_DATABASE_NAME = "experience-repository-test.db"
    }
}
