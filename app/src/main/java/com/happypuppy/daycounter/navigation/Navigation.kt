package com.happypuppy.daycounter.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Main : Screen("main_screen")
    object Settings : Screen("settings_screen")
    object CategoryManagement : Screen("category_management_screen")
}