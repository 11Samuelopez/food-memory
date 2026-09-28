package com.example.foodmemory.feature.nearby

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodmemory.domain.model.NearbyRestaurant
import com.example.foodmemory.domain.usecase.FindNearbyPlacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyState(
    val isLoading: Boolean = false,
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val restaurants: List<NearbyRestaurant> = emptyList(),
    val filter: NearbyFilter = NearbyFilter.ALL,
    val sort: NearbySort = NearbySort.DISTANCE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val message: NearbyMessage? = null
)

enum class NearbyMessage { LOCATION_UNAVAILABLE, PERMISSION_REQUIRED, NETWORK_ERROR, EMPTY }
enum class NearbyFilter { ALL, BAR, RESTAURANT, CAFE }
enum class NearbySort { DISTANCE, NAME }
enum class SortDirection { ASCENDING, DESCENDING }

/**
 * Presentation-layer coordinator for live location and nearby-place search.
 * GPS updates move the user marker continuously; network searches are throttled so
 * walking a few metres never causes a stream of requests to the public places API.
 */
class NearbyViewModel(
    application: Application,
    private val findNearbyPlaces: FindNearbyPlacesUseCase
) : AndroidViewModel(application) {
    private val locationManager = application.getSystemService(LocationManager::class.java)
    private val locationHandler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(NearbyState())
    val state = mutableState.asStateFlow()

    private var locationListener: LocationListener? = null
    private var locationTimeout: Runnable? = null
    private var isPlacesRequestRunning = false
    private var lastPlacesLocation: Location? = null
    private var lastPlacesRequestAt = 0L
    private var refreshAfterActiveRequest = false

    /** Starts foreground-only location updates while the map screen is visible. */
    fun startTracking() {
        if (locationListener != null) return

        val app = getApplication<Application>()
        val fineLocation = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineLocation && !coarseLocation) {
            mutableState.value = mutableState.value.copy(message = NearbyMessage.PERMISSION_REQUIRED)
            return
        }

        val providers = runCatching { locationManager.getProviders(true) }.getOrDefault(emptyList())
            .filter { provider -> provider == LocationManager.NETWORK_PROVIDER || (fineLocation && provider == LocationManager.GPS_PROVIDER) }
        if (providers.isEmpty()) {
            mutableState.value = mutableState.value.copy(isLoading = false, message = NearbyMessage.LOCATION_UNAVAILABLE)
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocationReceived(location)
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) {
                val enabled = runCatching { locationManager.getProviders(true) }.getOrDefault(emptyList())
                if (enabled.none { it in providers }) {
                    mutableState.value = mutableState.value.copy(isLoading = false, message = NearbyMessage.LOCATION_UNAVAILABLE)
                }
            }
        }
        locationListener = listener
        mutableState.value = mutableState.value.copy(
            isLoading = !mutableState.value.hasLocation,
            message = null
        )

        var registered = false
        providers.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(provider, LOCATION_INTERVAL_MILLIS, LOCATION_MIN_DISTANCE_METERS, listener, Looper.getMainLooper())
            }.onSuccess { registered = true }
        }
        if (!registered) {
            stopTracking()
            mutableState.value = mutableState.value.copy(isLoading = false, message = NearbyMessage.LOCATION_UNAVAILABLE)
            return
        }

        locationTimeout = Runnable {
            if (!mutableState.value.hasLocation) {
                mutableState.value = mutableState.value.copy(isLoading = false, message = NearbyMessage.LOCATION_UNAVAILABLE)
            }
        }.also { locationHandler.postDelayed(it, LOCATION_TIMEOUT_MILLIS) }

        val lastKnown = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
        val age = lastKnown?.let { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos }
        if (lastKnown != null && age != null && age in 0..MAX_LAST_KNOWN_AGE_NANOS) {
            onLocationReceived(lastKnown)
        }
    }

    /** Reuses the Home warm-up; retry only when that initial nearby lookup failed or was empty. */
    fun onMapOpened() {
        startTracking()
        val current = mutableState.value
        if (!current.hasLocation || current.message !in setOf(NearbyMessage.NETWORK_ERROR, NearbyMessage.EMPTY)) return

        val latitude = current.latitude ?: return
        val longitude = current.longitude ?: return
        val latestLocation = Location("food-memory-map-open").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        requestNearbyPlaces(latestLocation, SystemClock.elapsedRealtime())
    }

    /** Stops updates when the map leaves the foreground; Android permission stays unchanged. */
    fun stopTracking() {
        locationListener?.let { listener -> runCatching { locationManager.removeUpdates(listener) } }
        locationListener = null
        locationTimeout?.let(locationHandler::removeCallbacks)
        locationTimeout = null
    }

    fun selectFilter(filter: NearbyFilter) {
        mutableState.value = mutableState.value.copy(filter = filter)
    }

    fun selectSort(sort: NearbySort) {
        mutableState.value = mutableState.value.copy(sort = sort)
    }

    fun toggleSortDirection() {
        val next = if (mutableState.value.sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        mutableState.value = mutableState.value.copy(sortDirection = next)
    }

    private fun onLocationReceived(location: Location) {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
        locationTimeout?.let(locationHandler::removeCallbacks)
        locationTimeout = null

        val current = mutableState.value
        val recalculatedPlaces = current.restaurants.map { place ->
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, place.latitude, place.longitude, distance)
            place.copy(distanceMeters = distance[0].toInt())
        }
        mutableState.value = current.copy(
            hasLocation = true,
            latitude = location.latitude,
            longitude = location.longitude,
            restaurants = recalculatedPlaces,
            isLoading = isPlacesRequestRunning,
            message = if (current.message == NearbyMessage.LOCATION_UNAVAILABLE || current.message == NearbyMessage.PERMISSION_REQUIRED) null else current.message
        )

        val now = SystemClock.elapsedRealtime()
        val movedMeters = lastPlacesLocation?.distanceTo(location) ?: Float.MAX_VALUE
        val shouldSearch = lastPlacesLocation == null || movedMeters >= MIN_METERS_BETWEEN_SEARCHES || now - lastPlacesRequestAt >= PLACES_REFRESH_INTERVAL_MILLIS
        if (shouldSearch) requestNearbyPlaces(location, now)
    }

    private fun requestNearbyPlaces(location: Location, now: Long) {
        if (isPlacesRequestRunning) {
            refreshAfterActiveRequest = true
            return
        }

        lastPlacesLocation = Location(location)
        lastPlacesRequestAt = now
        isPlacesRequestRunning = true
        mutableState.value = mutableState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            runCatching { findNearbyPlaces(location.latitude, location.longitude) }
                .onSuccess { places ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        restaurants = places,
                        message = if (places.isEmpty()) NearbyMessage.EMPTY else null
                    )
                }
                .onFailure { error ->
                    Log.w(TAG, "Nearby places lookup failed", error)
                    mutableState.value = mutableState.value.copy(isLoading = false, message = NearbyMessage.NETWORK_ERROR)
                }

            isPlacesRequestRunning = false
            if (refreshAfterActiveRequest) {
                refreshAfterActiveRequest = false
                val latest = mutableState.value
                val latestLocation = latest.latitude?.let { latitude ->
                    latest.longitude?.let { longitude -> Location("food-memory-live").apply { this.latitude = latitude; this.longitude = longitude } }
                }
                if (latestLocation != null) requestNearbyPlaces(latestLocation, SystemClock.elapsedRealtime())
            }
        }
    }

    override fun onCleared() {
        stopTracking()
        super.onCleared()
    }

    private companion object {
        const val TAG = "FoodMemoryNearby"
        const val LOCATION_INTERVAL_MILLIS = 5_000L
        const val LOCATION_MIN_DISTANCE_METERS = 20f
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
        const val MAX_LAST_KNOWN_AGE_NANOS = 30_000_000_000L
        const val MIN_METERS_BETWEEN_SEARCHES = 300f
        const val PLACES_REFRESH_INTERVAL_MILLIS = 180_000L
    }
}
