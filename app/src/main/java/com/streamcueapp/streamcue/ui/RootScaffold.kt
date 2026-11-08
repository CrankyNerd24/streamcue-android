package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("shows", "Shows", Icons.Default.Tv),
    Tab("movies", "Movies", Icons.Default.Movie),
    Tab("services", "Services", Icons.Default.Subscriptions),
    Tab("discover", "Discover", Icons.Default.Explore),
)

/**
 * The four-tab shell matching iOS's RootView TabView (Shows / Movies /
 * Services / Discover). Add Show is a destination in the same nav graph
 * rather than its own tab, so the bottom bar hides while it's open — it's
 * reached by push, not by a tab, same as iOS's add-a-show sheet.
 */
@Composable
fun RootScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTabRoute = backStackEntry?.destination?.hierarchy
        ?.firstOrNull { dest -> tabs.any { it.route == dest.route } }
        ?.route

    Scaffold(
        bottomBar = {
            if (currentTabRoute != null) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTabRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "shows",
            modifier = Modifier.padding(padding)
        ) {
            composable("shows") {
                HomeScreen(
                    onAddShow = { navController.navigate("addShow") },
                    onShowClick = { id -> navController.navigate("showDetail/$id") }
                )
            }
            composable("movies") {
                MoviesScreen(
                    onAddMovie = { navController.navigate("addMovie") },
                    onMovieClick = { id -> navController.navigate("movieDetail/$id") }
                )
            }
            composable("services") {
                ServicesScreen(onChooseServices = { navController.navigate("servicePicker") })
            }
            composable("discover") { DiscoverScreen() }
            composable("addShow") {
                AddShowScreen(onDone = { navController.popBackStack() })
            }
            composable("addMovie") {
                AddMovieScreen(onDone = { navController.popBackStack() })
            }
            composable("servicePicker") {
                ServicePickerScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = "showDetail/{tmdbId}",
                arguments = listOf(navArgument("tmdbId") { type = NavType.IntType })
            ) { entry ->
                val tmdbId = entry.arguments?.getInt("tmdbId") ?: return@composable
                ShowDetailScreen(tmdbId = tmdbId, onBack = { navController.popBackStack() })
            }
            composable(
                route = "movieDetail/{tmdbId}",
                arguments = listOf(navArgument("tmdbId") { type = NavType.IntType })
            ) { entry ->
                val tmdbId = entry.arguments?.getInt("tmdbId") ?: return@composable
                MovieDetailScreen(tmdbId = tmdbId, onBack = { navController.popBackStack() })
            }
        }
    }
}
