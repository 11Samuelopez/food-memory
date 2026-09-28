package com.example.foodmemory.feature.nearby

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.foodmemory.R
import com.example.foodmemory.domain.model.NearbyRestaurant
import com.example.foodmemory.ui.theme.FoodForest
import com.example.foodmemory.ui.theme.FoodMapBackground
import java.util.Locale

@Composable
fun NearbyScreen(
    state: NearbyState,
    onMapOpened: () -> Unit,
    onFilterChange: (NearbyFilter) -> Unit,
    onSortChange: (NearbySort) -> Unit,
    onToggleSortDirection: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) onMapOpened()
    }

    fun requestLocationPermissionAndStartTracking() {
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) onMapOpened() else locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }

    LaunchedEffect(Unit) { requestLocationPermissionAndStartTracking() }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredPlaces = state.restaurants.filter { place ->
        val matchesCategory = when (state.filter) {
            NearbyFilter.ALL -> true
            NearbyFilter.BAR -> place.category == "bar" || place.category == "pub"
            NearbyFilter.RESTAURANT -> place.category == "restaurant"
            NearbyFilter.CAFE -> place.category == "cafe" || place.category == "fast_food"
        }
        matchesCategory && (searchQuery.isBlank() || place.name.contains(searchQuery.trim(), ignoreCase = true) || place.address?.contains(searchQuery.trim(), ignoreCase = true) == true)
    }
    val visiblePlaces = when (state.sort) {
        NearbySort.DISTANCE -> filteredPlaces.sortedBy { it.distanceMeters }
            .let { places -> if (state.sortDirection == SortDirection.DESCENDING) places.asReversed() else places }
        NearbySort.NAME -> sortBySpanishName(filteredPlaces, state.sortDirection)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.sort, state.sortDirection, state.filter, searchQuery) {
        listState.scrollToItem(0)
    }
    val mapPlaces = filteredPlaces.sortedBy { it.distanceMeters }.take(MAX_MAP_MARKERS)
    val deviceLocation = state.latitude?.takeIf(Double::isFinite)?.let { latitude ->
        state.longitude?.takeIf(Double::isFinite)?.let { longitude -> latitude to longitude }
    }?.takeIf { (latitude, longitude) -> latitude in -90.0..90.0 && longitude in -180.0..180.0 }

    Box(Modifier.fillMaxSize()) {
        if (deviceLocation != null) {
            NearbyMap(Modifier.fillMaxSize(), deviceLocation.first, deviceLocation.second, mapPlaces)
        } else {
            Box(Modifier.fillMaxSize().background(FoodMapBackground), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.isLoading) CircularProgressIndicator(color = FoodForest)
                    Text(
                        stringResource(if (state.isLoading) R.string.nearby_waiting_location else R.string.nearby_location_unavailable),
                        modifier = Modifier.padding(horizontal = 36.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 5.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium, color = FoodForest)
                }
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text(stringResource(R.string.nearby_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Text("⌕", color = FoodForest, style = MaterialTheme.typography.titleLarge) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shadowElevation = 14.dp
        ) {
            Column(Modifier.padding(top = 10.dp, bottom = 8.dp)) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(width = 34.dp, height = 4.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.nearby_list_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.isLoading) stringResource(R.string.nearby_searching)
                            else stringResource(R.string.nearby_results_count, filteredPlaces.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NearbyFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(stringResource(filter.labelResource())) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.nearby_sort_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NearbySort.entries.forEach { sort ->
                        FilterChip(
                            selected = state.sort == sort,
                            onClick = { onSortChange(sort) },
                            label = { Text(stringResource(sort.labelResource())) }
                        )
                    }
                    IconButton(onClick = onToggleSortDirection) {
                        Text(if (state.sortDirection == SortDirection.ASCENDING) "↑" else "↓", color = FoodForest, style = MaterialTheme.typography.titleLarge)
                    }
                }

                val message = state.message
                if (message != null) {
                    val text = when (message) {
                        NearbyMessage.LOCATION_UNAVAILABLE -> R.string.nearby_location_unavailable
                        NearbyMessage.PERMISSION_REQUIRED -> R.string.nearby_permission_required
                        NearbyMessage.NETWORK_ERROR -> R.string.nearby_network_error
                        NearbyMessage.EMPTY -> R.string.nearby_empty
                    }
                    Text(stringResource(text), Modifier.padding(horizontal = 22.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (!state.isLoading) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visiblePlaces, key = NearbyRestaurant::id) { restaurant -> RestaurantRow(restaurant) }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = FoodForest)
                    }
                }
                Text(
                    stringResource(R.string.nearby_map_attribution),
                    modifier = Modifier.padding(start = 20.dp, top = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun NearbyFilter.labelResource(): Int = when (this) {
    NearbyFilter.ALL -> R.string.nearby_filter_all
    NearbyFilter.BAR -> R.string.nearby_filter_bars
    NearbyFilter.RESTAURANT -> R.string.nearby_filter_restaurants
    NearbyFilter.CAFE -> R.string.nearby_filter_cafes
}

@Composable
private fun NearbySort.labelResource(): Int = when (this) {
    NearbySort.DISTANCE -> R.string.nearby_sort_distance
    NearbySort.NAME -> R.string.nearby_sort_name
}

@Composable
private fun RestaurantRow(restaurant: NearbyRestaurant) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(restaurant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                restaurant.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
            Text(formatDistance(restaurant.distanceMeters), style = MaterialTheme.typography.labelMedium, color = FoodForest, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatDistance(meters: Int): String = if (meters >= 1000) String.format(Locale.getDefault(), "%.1f km", meters / 1000.0) else "$meters m"

private const val MAX_MAP_MARKERS = 200
