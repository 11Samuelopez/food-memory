package com.example.foodmemory.feature.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.foodmemory.R
import com.example.foodmemory.feature.shared.PlaceholderScreen

@Composable
fun MenuScanScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = stringResource(R.string.menu_scan_title),
    message = stringResource(R.string.menu_scan_message),
    actionLabel = stringResource(R.string.menu_scan_back),
    onAction = onBack
)
