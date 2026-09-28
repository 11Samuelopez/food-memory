package com.example.foodmemory.feature.experience

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodmemory.domain.model.FoodExperience
import com.example.foodmemory.domain.model.NewExperience
import com.example.foodmemory.domain.usecase.AddExperienceUseCase
import com.example.foodmemory.domain.usecase.ObserveExperiencesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AddExperienceEvent {
    data object Saved : AddExperienceEvent
    data object Failed : AddExperienceEvent
}

class ExperienceViewModel(
    observeExperiences: ObserveExperiencesUseCase,
    private val addExperienceUseCase: AddExperienceUseCase
) : ViewModel() {
    val experiences = observeExperiences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<FoodExperience>()
    )

    private val eventChannel = Channel<AddExperienceEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun addExperience(experience: NewExperience) {
        viewModelScope.launch {
            try {
                addExperienceUseCase(experience)
                eventChannel.send(AddExperienceEvent.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                eventChannel.send(AddExperienceEvent.Failed)
            }
        }
    }
}
