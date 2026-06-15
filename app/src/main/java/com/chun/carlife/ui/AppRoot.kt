package com.chun.carlife.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chun.carlife.ui.theme.CarLifeTheme
import com.chun.carlife.ui.vehicles.VehicleEditScreen
import com.chun.carlife.ui.vehicles.VehicleListScreen
import com.chun.carlife.ui.refuel.RefuelScreen
import com.chun.carlife.ui.refuel.RefuelEditScreen
import com.chun.carlife.ui.maintenance.MaintenanceScreen
import com.chun.carlife.ui.maintenance.MaintenanceEditScreen
import com.chun.carlife.ui.stats.StatsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("vehicles", "車両", Icons.Filled.DirectionsCar),
    TabItem("refuel", "給油", Icons.Filled.LocalGasStation),
    TabItem("maintenance", "整備", Icons.Filled.Build),
    TabItem("stats", "集計", Icons.Filled.BarChart),
)

@Composable
fun AppRoot() {
    CarLifeTheme {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                if (tabs.any { currentRoute?.startsWith(it.route) == true }) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = backStackEntry?.destination?.hierarchy?.any {
                                it.route?.startsWith(tab.route) == true
                            } == true
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
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding: PaddingValues ->
            NavHost(
                navController = navController,
                startDestination = "vehicles",
                modifier = Modifier.padding(padding),
            ) {
                composable("vehicles") {
                    VehicleListScreen(
                        onAdd = { navController.navigate("vehicleEdit/0") },
                        onEdit = { id -> navController.navigate("vehicleEdit/$id") },
                    )
                }
                composable("vehicleEdit/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    VehicleEditScreen(vehicleId = id, onDone = { navController.popBackStack() })
                }
                composable("refuel") {
                    RefuelScreen(
                        onAdd = { vehicleId -> navController.navigate("refuelEdit/$vehicleId/0") },
                        onEdit = { vehicleId, id -> navController.navigate("refuelEdit/$vehicleId/$id") },
                    )
                }
                composable("refuelEdit/{vehicleId}/{id}") { backStackEntry ->
                    val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull() ?: 0L
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    RefuelEditScreen(
                        vehicleId = vehicleId,
                        refuelId = id,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable("maintenance") {
                    MaintenanceScreen(
                        onAdd = { vehicleId -> navController.navigate("maintenanceEdit/$vehicleId/0") },
                        onEdit = { vehicleId, id -> navController.navigate("maintenanceEdit/$vehicleId/$id") },
                    )
                }
                composable("maintenanceEdit/{vehicleId}/{id}") { backStackEntry ->
                    val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull() ?: 0L
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    MaintenanceEditScreen(
                        vehicleId = vehicleId,
                        maintenanceId = id,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable("stats") { StatsScreen() }
            }
        }
    }
}
