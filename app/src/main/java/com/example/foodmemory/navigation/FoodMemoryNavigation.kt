package com.example.foodmemory.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.foodmemory.app.FoodMemoryScaffold
import com.example.foodmemory.app.FoodMemorySplashLoading
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodmemory.app.AppContainer
import com.example.foodmemory.feature.experience.ExperienceViewModel
import com.example.foodmemory.feature.dish.DishCaptureScreen
import com.example.foodmemory.feature.dish.DishCaptureViewModel
import com.example.foodmemory.feature.experience.AddExperienceScreen
import com.example.foodmemory.feature.home.HomeScreen
import com.example.foodmemory.feature.menu.MenuScanScreen
import com.example.foodmemory.feature.profile.ProfileScreen
import com.example.foodmemory.feature.profile.ProfileViewModel
import com.example.foodmemory.feature.session.SessionViewModel
import com.example.foodmemory.feature.session.SessionScreen
import com.example.foodmemory.feature.nearby.NearbyScreen
import com.example.foodmemory.feature.nearby.NearbyViewModel
import com.example.foodmemory.feature.nearby.NearbyMap
import com.example.foodmemory.R
import com.example.foodmemory.navigation.AppRoute.AddExperience
import com.example.foodmemory.navigation.AppRoute.DishCapture
import com.example.foodmemory.navigation.AppRoute.Home
import com.example.foodmemory.navigation.AppRoute.MenuScan
import com.example.foodmemory.navigation.AppRoute.Nearby
import com.example.foodmemory.navigation.AppRoute.Profile
import com.example.foodmemory.navigation.AppRoute.Session

sealed class AppRoute(val route: String, val labelResource: Int, val icon: String) {
    data object Home : AppRoute("home", R.string.nav_home, "⌂")
    data object Profile : AppRoute("profile", R.string.nav_profile, "✦")
    data object Nearby : AppRoute("nearby", R.string.nav_nearby, "⌖")
    data object AddExperience : AppRoute("experience/new", R.string.experience_title, "＋")
    data object MenuScan : AppRoute("menu/scan", R.string.menu_scan_title, "⌕")
    data object DishCapture : AppRoute("dish/capture", R.string.dish_capture_title, "◉")
    data object Session : AppRoute("session", R.string.session_title, "○")
}

private val mainRoutes = listOf(Home, Nearby, Profile)

@Composable
fun FoodMemoryNavHost(navController: NavHostController, appContainer: AppContainer, sessionViewModel: SessionViewModel) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
    val nearbyViewModel: NearbyViewModel = viewModel(factory = appContainer.nearbyViewModelFactory)
    val nearbyState by nearbyViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        sessionViewModel.consumeLocationPermissionRequest()
        if (it.values.any { granted -> granted } && sessionState.user != null) {
            nearbyViewModel.startTracking()
        }
    }
    val selectedMainRoute = mainRoutes.firstOrNull { it.route == currentRoute } ?: Home

    if (sessionState.isLoading) {
        FoodMemorySplashLoading()
        return
    }

    LaunchedEffect(sessionState.user?.userId) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (sessionState.user != null && hasLocationPermission) {
            // Warm device location and nearby results while the user is on Home.
            nearbyViewModel.startTracking()
        } else if (sessionState.user == null) {
            nearbyViewModel.stopTracking()
        }
    }

    LaunchedEffect(sessionState.user?.userId, currentRoute) {
        if (sessionState.user != null && currentRoute == Session.route) {
            navController.navigate(Home.route) {
                popUpTo(Session.route) { inclusive = true }
                launchSingleTop = true
            }
        } else if (sessionState.user == null && currentRoute != null && currentRoute != Session.route) {
            navController.navigate(Session.route) {
                popUpTo(Home.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(sessionState.shouldRequestLocationPermission) {
        if (sessionState.shouldRequestLocationPermission) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasLocationPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            } else {
                nearbyViewModel.startTracking()
                sessionViewModel.consumeLocationPermissionRequest()
            }
        }
    }

    FoodMemoryScaffold(
        selectedRoute = selectedMainRoute,
        showBottomBar = currentRoute in mainRoutes.map { it.route } && currentRoute != Nearby.route,
        onNavigate = { route ->
            navController.navigate(route.route) {
                popUpTo(Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val warmupLatitude = nearbyState.latitude?.takeIf(Double::isFinite)
            val warmupLongitude = nearbyState.longitude?.takeIf(Double::isFinite)
            if (sessionState.user != null && currentRoute != Nearby.route &&
                warmupLatitude != null && warmupLongitude != null
            ) {
                // Build the MapLibre style and warm visible map tiles behind the active screen.
                // NearbyScreen gets its own correctly-sized map when opened; this view is disposable prefetch.
                NearbyMap(
                    // Keep the render surface active so MapLibre fills its disk tile cache.
                    modifier = Modifier.fillMaxSize().alpha(0f).clearAndSetSemantics { },
                    latitude = warmupLatitude,
                    longitude = warmupLongitude,
                    restaurants = nearbyState.restaurants.take(200)
                )
            }
            NavHost(
                navController = navController,
                startDestination = if (sessionState.user != null) Home.route else Session.route,
                modifier = Modifier.fillMaxSize()
            ) {
            composable(Session.route) {
                val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
                SessionScreen(
                    state = sessionState,
                    onSignIn = sessionViewModel::signIn
                )
            }
            composable(Home.route) {
                val viewModel: ExperienceViewModel = viewModel(factory = appContainer.experienceViewModelFactory)
                val experiences by viewModel.experiences.collectAsStateWithLifecycle()
                HomeScreen(
                    experiences = experiences,
                    onAddExperience = { navController.navigate(AddExperience.route) },
                    onCaptureDish = { navController.navigate(DishCapture.route) }
                )
            }
            composable(Profile.route) {
                val viewModel: ProfileViewModel = viewModel(factory = appContainer.profileViewModelFactory)
                val profileState by viewModel.state.collectAsStateWithLifecycle()
                ProfileScreen(state = profileState, user = sessionState.user, onSignOut = sessionViewModel::signOut)
            }
            composable(Nearby.route) {
                NearbyScreen(
                    state = nearbyState,
                    onMapOpened = nearbyViewModel::onMapOpened,
                    onFilterChange = nearbyViewModel::selectFilter,
                    onSortChange = nearbyViewModel::selectSort,
                    onToggleSortDirection = nearbyViewModel::toggleSortDirection,
                    onBack = { navController.navigate(Home.route) { launchSingleTop = true } }
                )
            }
            composable(AddExperience.route) { experienceEntry ->
                val viewModel: ExperienceViewModel = viewModel(factory = appContainer.experienceViewModelFactory)
                val capturedDishFlow = remember(experienceEntry) {
                    experienceEntry.savedStateHandle.getStateFlow<String?>(CAPTURED_DISH_RESULT_KEY, null)
                }
                val capturedDishJson by capturedDishFlow.collectAsStateWithLifecycle()
                AddExperienceScreen(
                    viewModel = viewModel,
                    capturedDishJson = capturedDishJson,
                    onConsumeCapturedDish = { experienceEntry.savedStateHandle[CAPTURED_DISH_RESULT_KEY] = null },
                    onAddDish = { navController.navigate(DishCapture.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(DishCapture.route) {
                val viewModel: DishCaptureViewModel = viewModel(factory = appContainer.dishCaptureViewModelFactory)
                DishCaptureScreen(
                    viewModel = viewModel,
                    onConfirm = { dish ->
                        val result = CapturedDishCodec.encode(dish)
                        if (navController.previousBackStackEntry?.destination?.route == AddExperience.route) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(CAPTURED_DISH_RESULT_KEY, result)
                            navController.popBackStack()
                        } else {
                            navController.navigate(AddExperience.route) {
                                popUpTo(DishCapture.route) { inclusive = true }
                            }
                            navController.currentBackStackEntry?.savedStateHandle?.set(CAPTURED_DISH_RESULT_KEY, result)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(MenuScan.route) {
                MenuScanScreen(onBack = { navController.popBackStack() })
            }
            }
        }
    }
}

private const val CAPTURED_DISH_RESULT_KEY = "captured_dish_json"
