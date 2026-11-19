package com.streamcast.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamcast.mobile.ui.screens.CastDevicePickerModal
import com.streamcast.mobile.ui.screens.DeviceDiscoveryScreen
import com.streamcast.mobile.ui.screens.FavoritesScreen
import com.streamcast.mobile.ui.screens.HomeScreen
import com.streamcast.mobile.ui.screens.LiveTvScreen
import com.streamcast.mobile.ui.screens.PlayerScreen
import com.streamcast.mobile.ui.screens.SettingsScreen
import com.streamcast.mobile.ui.screens.StreamCastMockData
import com.streamcast.mobile.ui.screens.WifiRemoteScreen
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel

sealed class StreamCastRoute(val route: String, val label: String) {
    data object Home : StreamCastRoute("home", "Home")
    data object LiveTv : StreamCastRoute("live_tv", "Live TV")
    data object Favorites : StreamCastRoute("favorites", "Favorites")
    data object Remote : StreamCastRoute("remote", "Remote")
    data object Settings : StreamCastRoute("settings", "Settings")
    data object Player : StreamCastRoute("player/{channelId}", "Player")
    data object DeviceDiscovery : StreamCastRoute("device_discovery", "Connect TV")
}

@Composable
fun StreamCastNavHost(castViewModel: CastViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showCastDialog by castViewModel.showCastDialog.collectAsState()
    val connectedDevice by castViewModel.connectedDevice.collectAsState()

    val primaryTabs = listOf(
        StreamCastRoute.Home to Icons.Rounded.Home,
        StreamCastRoute.LiveTv to Icons.Rounded.LiveTv,
        StreamCastRoute.Favorites to Icons.Rounded.Favorite,
        StreamCastRoute.Remote to Icons.Rounded.SettingsRemote,
        StreamCastRoute.Settings to Icons.Rounded.Settings
    )

    Scaffold(
        containerColor = StreamCastColors.Background,
        bottomBar = {
            if (primaryTabs.any { it.first.route == currentRoute || currentRoute?.startsWith("live_tv") == true }) {
                NavigationBar(containerColor = StreamCastColors.SurfaceSecondary.copy(alpha = 0.92f)) {
                    primaryTabs.forEach { (tab, icon) ->
                        val selected = currentRoute == tab.route || (tab.route == "live_tv" && currentRoute?.startsWith("live_tv") == true)
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
                            label = { Text(tab.label) },
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(StreamCastRoute.Home.route) {
                HomeScreen(
                    connectedDevice = connectedDevice,
                    onBrowseChannels = { navController.navigate("live_tv") },
                    onCategoryClick = { catName -> navController.navigate("live_tv?category=$catName") },
                    onOpenDiscovery = { castViewModel.openCastDialog() },
                    onDisconnect = { castViewModel.disconnectDevice() },
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                    onOpenRemote = { navController.navigate(StreamCastRoute.Remote.route) }
                )
            }
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
                val categoryName = entry.arguments?.getString("category")
                LiveTvScreen(
                    initialCategoryName = categoryName,
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                    onCast = { id ->
                        val ch = StreamCastMockData.getChannelById(id).channel
                        castViewModel.openCastDialog(ch)
                    }
                )
            }
            composable(StreamCastRoute.Favorites.route) {
                FavoritesScreen(
                    onExplore = { navController.navigate("live_tv") },
                    onOpenPlayer = { id -> navController.navigate("player/$id") },
                    onCast = { id ->
                        val ch = StreamCastMockData.getChannelById(id).channel
                        castViewModel.openCastDialog(ch)
                    }
                )
            }
            composable(StreamCastRoute.Remote.route) {
                WifiRemoteScreen(
                    onFindTv = { castViewModel.openCastDialog() }
                )
            }
            composable(StreamCastRoute.Settings.route) {
                SettingsScreen()
            }
            composable(StreamCastRoute.DeviceDiscovery.route) {
                DeviceDiscoveryScreen(
                    onBack = { navController.popBackStack() },
                    onLaunchRemote = { navController.navigate(StreamCastRoute.Remote.route) }
                )
            }
            composable(StreamCastRoute.Player.route) { entry ->
                val channelId = entry.arguments?.getString("channelId").orEmpty()
                PlayerScreen(
                    channelId = channelId,
                    connectedDevice = connectedDevice,
                    onBack = { navController.popBackStack() },
                    onOpenDiscovery = {
                        val ch = StreamCastMockData.getChannelById(channelId).channel
                        castViewModel.openCastDialog(ch)
                    }
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
