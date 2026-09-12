package com.happypuppy.daycounter.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.ui.screens.CategoryManagementScreen
import com.happypuppy.daycounter.ui.screens.MainScreen
import com.happypuppy.daycounter.ui.screens.SettingsScreen
import com.happypuppy.daycounter.ui.screens.SplashScreen
import kotlinx.coroutines.delay

/** How long the splash destination stays visible before navigating to Main. */
private const val SPLASH_DURATION_MS = 1500L

@Composable
fun AppNavigation() {
    // rememberNavController saves and restores the back stack itself, so once the
    // splash destination has navigated to Main (popping itself), a configuration
    // change or process death restores Main directly. Splash is therefore the single
    // fixed start destination and the splash composable owns the only timer (P2-8).
    val navController = rememberNavController()

    // Define animation durations and easing
    val animDuration = 400 // Animation duration in milliseconds

    NavHost(
        navController = navController,
        startDestination = Screen.Splash
    ) {
        composable<Screen.Splash>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
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
            SplashScreen()

            LaunchedEffect(key1 = true) {
                delay(SPLASH_DURATION_MS)
                if (BuildConfig.DEBUG) Log.d("Navigation", "Navigating from Splash to Main")
                navController.navigate(Screen.Main) {
                    popUpTo<Screen.Splash> { inclusive = true }
                }
            }
        }

        composable<Screen.Main>(
            enterTransition = {
                if (initialState.destination.hasRoute<Screen.Splash>()) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(animDuration)
                    )
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(animDuration)
                    )
                }
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
            MainScreen(navController = navController)
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
            SettingsScreen(navController = navController)
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
            CategoryManagementScreen(navController = navController)
        }
    }
}
