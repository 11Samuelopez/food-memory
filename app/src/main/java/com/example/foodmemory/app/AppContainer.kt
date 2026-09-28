package com.example.foodmemory.app

import com.example.foodmemory.BuildConfig
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodmemory.data.local.FoodMemoryDatabase
import com.example.foodmemory.data.local.InternalDishPhotoRepository
import com.example.foodmemory.data.local.PreferencesSessionRepository
import com.example.foodmemory.data.demo.DemoDishAnalysisRepository
import com.example.foodmemory.data.demo.DemoTasteRecommendationRepository
import com.example.foodmemory.data.remote.HttpTasteRecommendationRepository
import com.example.foodmemory.data.repository.RoomExperienceRepository
import com.example.foodmemory.domain.usecase.AnalyzeDishPhotoUseCase
import com.example.foodmemory.domain.usecase.AddExperienceUseCase
import com.example.foodmemory.domain.usecase.CacheDishPhotoUseCase
import com.example.foodmemory.domain.usecase.GetTasteProfileUseCase
import com.example.foodmemory.domain.usecase.GetAiTasteRecommendationsUseCase
import com.example.foodmemory.domain.usecase.ObserveExperiencesUseCase
import com.example.foodmemory.domain.usecase.GetSessionUseCase
import com.example.foodmemory.domain.usecase.StartSessionUseCase
import com.example.foodmemory.domain.usecase.ClearSessionUseCase
import com.example.foodmemory.feature.dish.DishCaptureViewModel
import com.example.foodmemory.domain.repository.ExperienceRepository
import com.example.foodmemory.feature.experience.ExperienceViewModel
import com.example.foodmemory.feature.profile.ProfileViewModel
import com.example.foodmemory.feature.session.SessionViewModel
import com.example.foodmemory.feature.nearby.NearbyViewModel
import com.example.foodmemory.data.remote.OverpassRestaurantDataSource
import com.example.foodmemory.domain.usecase.FindNearbyPlacesUseCase

class AppContainer(context: Context) {
    private val database = FoodMemoryDatabase.getInstance(context)
    private val experienceRepository: ExperienceRepository = RoomExperienceRepository(database)
    private val sessionRepository = PreferencesSessionRepository(context.applicationContext)

    val sessionViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
                return SessionViewModel(
                    GetSessionUseCase(sessionRepository),
                    StartSessionUseCase(sessionRepository),
                    ClearSessionUseCase(sessionRepository)
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    val experienceViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExperienceViewModel::class.java)) {
                return ExperienceViewModel(
                    ObserveExperiencesUseCase(experienceRepository),
                    AddExperienceUseCase(experienceRepository)
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    val profileViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                // Composition root: remote Gemini-backed API plus grounded offline suggestions.
                val localRecommendations = DemoTasteRecommendationRepository()
                val aiRecommendations = HttpTasteRecommendationRepository(BuildConfig.DISH_AI_API_BASE_URL)
                return ProfileViewModel(
                    ObserveExperiencesUseCase(experienceRepository),
                    GetTasteProfileUseCase(),
                    GetAiTasteRecommendationsUseCase(aiRecommendations),
                    GetAiTasteRecommendationsUseCase(localRecommendations)
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    val dishCaptureViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DishCaptureViewModel::class.java)) {
                // TODO: sustituir por HttpDishAnalysisRepository cuando se habilite visión IA.
                val analysisRepository = DemoDishAnalysisRepository()
                val photoRepository = InternalDishPhotoRepository(context.applicationContext)
                return DishCaptureViewModel(
                    AnalyzeDishPhotoUseCase(analysisRepository),
                    CacheDishPhotoUseCase(photoRepository)
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    val nearbyViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NearbyViewModel::class.java)) {
                val repository = OverpassRestaurantDataSource()
                return NearbyViewModel(
                    context.applicationContext as android.app.Application,
                    FindNearbyPlacesUseCase(repository)
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
