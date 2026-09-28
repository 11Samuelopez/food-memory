package com.example.foodmemory

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.foodmemory.app.FoodMemoryApp
import com.example.foodmemory.ui.theme.FoodMemoryTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    /** Spanish is the presentation language for this demo; locale resources keep future translations ready. */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.forLanguageTag(ACTIVE_LANGUAGE_TAG)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodMemoryTheme {
                FoodMemoryApp()
            }
        }
    }

    private companion object {
        const val ACTIVE_LANGUAGE_TAG = "es"
    }
}
