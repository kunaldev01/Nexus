package com.example.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.viewmodel.LifeOsViewModel

/**
 * The implicit central State Provider for Jetpack Compose - the native Android direct equivalent 
 * to the React Context API. This system allows components in any deep sub-hierarchy (fitness, 
 * nutrition, etc.) to access the centralized View Model state without prop drilling.
 */
val LocalLifeOsViewModel = staticCompositionLocalOf<LifeOsViewModel> {
    error("LocalLifeOsViewModel not provided. Make sure to wrap your UI inside a LifeOsStateProvider block.")
}

@Composable
fun LifeOsStateProvider(
    viewModel: LifeOsViewModel,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLifeOsViewModel provides viewModel
    ) {
        content()
    }
}
