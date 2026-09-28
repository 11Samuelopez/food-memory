package com.example.foodmemory.app

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmemory.navigation.FoodMemoryNavHost
import com.example.foodmemory.feature.session.SessionViewModel

@Composable
fun FoodMemoryApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = remember(context) { AppContainer(context.applicationContext) }
    val sessionViewModel: SessionViewModel = viewModel(factory = appContainer.sessionViewModelFactory)
    FoodMemoryNavHost(navController = navController, appContainer = appContainer, sessionViewModel = sessionViewModel)
}
