package com.happypuppy.daycounter.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Compose Navigation routes (Navigation 2.8+ / P2-7).
 * Each destination is a `@Serializable` object used with `composable<T>()` and `navigate(T)`.
 */
sealed interface Screen {
    @Serializable
    data object Main : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object CategoryManagement : Screen
}
