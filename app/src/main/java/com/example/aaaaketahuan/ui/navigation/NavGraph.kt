package com.example.aaaaketahuan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aaaaketahuan.ui.dashboard.DashboardScreen
import com.example.aaaaketahuan.ui.export.ExportImportScreen
import com.example.aaaaketahuan.ui.grafik.GrafikScreen
import com.example.aaaaketahuan.ui.input.InputTransaksiScreen
import com.example.aaaaketahuan.ui.riwayat.RiwayatScreen
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoute.DASHBOARD, "Dashboard", Icons.Filled.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    BottomNavItem(NavRoute.INPUT, "Input", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
    BottomNavItem(NavRoute.RIWAYAT, "Riwayat", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem(NavRoute.GRAFIK, "Grafik", Icons.Filled.Analytics, Icons.Outlined.Analytics)
)

@Composable
fun AppNavGraph(
    viewModel: TransaksiViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                unselectedTextColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.DASHBOARD,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoute.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRiwayat = {
                        navController.navigate(NavRoute.RIWAYAT) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGrafik = {
                        navController.navigate(NavRoute.GRAFIK) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToExport = {
                        navController.navigate(NavRoute.EXPORT_IMPORT)
                    },
                    onNavigateToInput = {
                        navController.navigate(NavRoute.INPUT) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(NavRoute.INPUT) {
                InputTransaksiScreen(viewModel = viewModel)
            }

            composable(NavRoute.RIWAYAT) {
                RiwayatScreen(
                    viewModel = viewModel,
                    onEditTransaksi = { transaksiId ->
                        navController.navigate(NavRoute.editTransaksi(transaksiId))
                    }
                )
            }

            composable(NavRoute.GRAFIK) {
                GrafikScreen(viewModel = viewModel)
            }

            composable(NavRoute.EXPORT_IMPORT) {
                ExportImportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoute.EDIT_TRANSAKSI,
                arguments = listOf(navArgument("transaksiId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transaksiId = backStackEntry.arguments?.getString("transaksiId") ?: ""
                InputTransaksiScreen(
                    viewModel = viewModel,
                    editTransaksiId = transaksiId
                )
            }
        }
    }
}
