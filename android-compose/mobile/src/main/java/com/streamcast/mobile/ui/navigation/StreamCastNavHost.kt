package com.streamcast.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamcast.mobile.ui.screens.CastDevicePickerModal
import com.streamcast.mobile.ui.screens.FavoritesScreen
import com.streamcast.mobile.ui.screens.HomeScreen
import com.streamcast.mobile.ui.screens.LiveTvScreen
import com.streamcast.mobile.ui.screens.PlayerScreen
import com.streamcast.mobile.ui.screens.SettingsScreen
import com.streamcast.mobile.ui.screens.WifiRemoteScreen
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel

sealed class StreamCastRoute(val route: String, val label: String) {
    data object Home : StreamCastRoute("home", "Home")
    data object LiveTv : StreamCastRoute("live_tv", "Live TV")
    data object Favorites : StreamCastRoute("favorites", "Favourites")
    data object Remote : StreamCastRoute("remote", "Remote")
    data object Settings : StreamCastRoute("settings", "Settings")
    data object Player : StreamCastRoute("player/{channelId}", "Player")
}

@Composable
fun StreamCastNavHost(
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showCastDialog by castViewModel.showCastDialog.collectAsState()
    val castMessage by castViewModel.message.collectAsState()
    val channelsToast by channelsViewModel.toast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Surface cast outcomes and playlist results in one place.
    LaunchedEffect(castMessage) {
        castMessage?.let {
            snackbarHostState.showSnackbar(it)
            castViewModel.consumeMessage()
        }
    }
    LaunchedEffect(channelsToast) {
        channelsToast?.let {
            snackbarHostState.showSnackbar(it)
            channelsViewModel.consumeToast()
        }
    }

    val primaryTabs = listOf(
        StreamCastRoute.Home to Icons.Rounded.Home,
        StreamCastRoute.LiveTv to Icons.Rounded.LiveTv,
        StreamCastRoute.Favorites to Icons.Rounded.Favorite,
        StreamCastRoute.Remote to Icons.Rounded.SettingsRemote,
        StreamCastRoute.Settings to Icons.Rounded.Settings
    )

    val isPrimaryTab = primaryTabs.any { matchesTab(currentRoute, it.first.route) }

    Scaffold(
        containerColor = StreamCastColors.Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StreamCastColors.SurfaceBright)
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        bottomBar = {
            if (isPrimaryTab) {
                NavigationBar(containerColor = StreamCastColors.SurfaceSecondary) {
                    primaryTabs.forEach { (tab, icon) ->
                        val selected = matchesTab(currentRoute, tab.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StreamCastColors.PrimaryViolet,
                                selectedTextColor = Color.White,
                                indicatorColor = StreamCastColors.PrimaryViolet.copy(alpha = 0.2f),
                                unselectedIconColor = StreamCastColors.TextSecondary,
                                unselectedTextColor = StreamCastColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StreamCastRoute.Home.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(StreamCastRoute.Home.route) {
                HomeScreen(
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel,
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                    onBrowseChannels = { navController.navigate(StreamCastRoute.LiveTv.route) },
                    onOpenCategory = { category ->
                        navController.navigate("live_tv?category=${category.displayName}")
                    },
                    onOpenRemote = { navController.navigate(StreamCastRoute.Remote.route) }
                )
            }

            // Single destination serving both "live_tv" and "live_tv?category=News"
            composable(
                route = "live_tv?category={category}",
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                LiveTvScreen(
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel,
                    initialCategoryName = entry.arguments?.getString("category"),
                    onOpenPlayer = { id -> navController.navigate("player/$id") }
                )
            }

            composable(StreamCastRoute.Favorites.route) {
                FavoritesScreen(
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel,
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                    onExplore = { navController.navigate(StreamCastRoute.LiveTv.route) }
                )
            }

            composable(StreamCastRoute.Remote.route) {
                WifiRemoteScreen(
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel,
                    onFindTv = { castViewModel.openCastDialog() }
                )
            }

            composable(StreamCastRoute.Settings.route) {
                SettingsScreen(channelsViewModel = channelsViewModel)
            }

            composable(StreamCastRoute.Player.route) { entry ->
                PlayerScreen(
                    channelId = entry.arguments?.getString("channelId").orEmpty(),
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenRemote = { navController.navigate(StreamCastRoute.Remote.route) }
                )
            }
        }

        if (showCastDialog) {
            CastDevicePickerModal(
                castViewModel = castViewModel,
                onDismissRequest = { castViewModel.closeCastDialog() },
                onOpenRemote = {
                    castViewModel.closeCastDialog()
                    navController.navigate(StreamCastRoute.Remote.route)
                }
            )
        }
    }
}

/**
 * The Live TV destination is registered with an optional query argument, so its
 * route string is "live_tv?category={category}" even when no category is set.
 * Tab selection has to match on the path portion.
 */
private fun matchesTab(currentRoute: String?, tabRoute: String): Boolean {
    if (currentRoute == null) return false
    return currentRoute == tabRoute || currentRoute.substringBefore('?') == tabRoute
}
