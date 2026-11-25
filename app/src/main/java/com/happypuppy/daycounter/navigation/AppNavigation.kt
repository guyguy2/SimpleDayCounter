package com.happypuppy.daycounter.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.happypuppy.daycounter.ui.screens.CategoryManagementScreen
import com.happypuppy.daycounter.ui.screens.MainScreen
import com.happypuppy.daycounter.ui.screens.SettingsScreen

@Composable
fun AppNavigation() {
    // rememberNavController saves and restores the back stack itself. Main is the start
    // destination: the system splash screen covers startup (see MainActivity).
    val navController = rememberNavController()

    // Define animation durations and easing
    val animDuration = 400 // Animation duration in milliseconds

    NavHost(
        navController = navController,
        startDestination = Screen.Main
    ) {
        composable<Screen.Main>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animDuration)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            }
        ) {
            MainScreen(
                onOpenSettings = { navController.navigate(Screen.Settings) }
            )
        }

        composable<Screen.Settings>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animDuration)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animDuration)
                )
            }
        ) {
            SettingsScreen(
                onBack = { navController.navigateUp() },
                onOpenCategories = { navController.navigate(Screen.CategoryManagement) }
            )
        }

        composable<Screen.CategoryManagement>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animDuration)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animDuration)
                )
            }
        ) {
            CategoryManagementScreen(onBack = { navController.navigateUp() })
        }
    }
}
