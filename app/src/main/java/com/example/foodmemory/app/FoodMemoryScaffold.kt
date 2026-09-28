package com.example.foodmemory.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodmemory.navigation.AppRoute
import com.example.foodmemory.navigation.AppRoute.Home
import com.example.foodmemory.navigation.AppRoute.Profile
import com.example.foodmemory.navigation.AppRoute.Nearby

@Composable
fun FoodMemoryScaffold(
    selectedRoute: AppRoute,
    showBottomBar: Boolean,
    onNavigate: (AppRoute) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    listOf(Home, Nearby, Profile).forEach { route ->
                        NavigationBarItem(
                            selected = selectedRoute == route,
                            onClick = { onNavigate(route) },
                            icon = { Text(route.icon, style = MaterialTheme.typography.titleLarge) },
                            label = { Text(stringResource(route.labelResource)) }
                        )
                    }
                }
            }
        },
        content = content
    )
}
