package com.chun.carlife.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.LaunchedEffect
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
import com.chun.carlife.ui.refuel.RefuelAddScreen
import com.chun.carlife.ui.maintenance.MaintenanceScreen
import com.chun.carlife.ui.maintenance.MaintenanceEditScreen
import com.chun.carlife.ui.settings.SettingsGeneralScreen
import com.chun.carlife.ui.settings.SettingsMaintenanceScreen
import com.chun.carlife.ui.settings.SettingsRefuelCsvImportScreen
import com.chun.carlife.ui.settings.SettingsRefuelScreen
import com.chun.carlife.ui.settings.SettingsScreen
import com.chun.carlife.ui.settings.SettingsStatsScreen
import com.chun.carlife.ui.settings.SettingsVehiclesScreen
import com.chun.carlife.ui.stats.StatsScreen

const val ACTION_ADD_REFUEL = "com.chun.carlife.action.ADD_REFUEL"

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("vehicles", "車両", Icons.Filled.DirectionsCar),
    TabItem("refuel", "給油", Icons.Filled.LocalGasStation),
    TabItem("maintenance", "整備", Icons.Filled.Build),
    TabItem("stats", "集計", Icons.Filled.BarChart),
)

@Composable
fun AppRoot(pendingAction: String? = null, onActionConsumed: () -> Unit = {}) {
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
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                val openSettings: () -> Unit = { navController.navigate("settings") }
                composable("vehicles") {
                    VehicleListScreen(
                        onAdd = { navController.navigate("vehicleEdit/0") },
                        onEdit = { id -> navController.navigate("vehicleEdit/$id") },
                        onOpenSettings = openSettings,
                    )
                }
                composable("vehicleEdit/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    VehicleEditScreen(vehicleId = id, onDone = { navController.popBackStack() })
                }
                composable("refuel") {
                    RefuelScreen(
                        onAdd = { vehicleId -> navController.navigate("refuelAdd/$vehicleId") },
                        onEdit = { vehicleId, id -> navController.navigate("refuelEdit/$vehicleId/$id") },
                        onOpenSettings = openSettings,
                    )
                }
                composable("refuelAdd/{vehicleId}") { backStackEntry ->
                    val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull() ?: 0L
                    RefuelAddScreen(
                        initialVehicleId = vehicleId,
                        onDone = { navController.popBackStack() },
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
                        onOpenSettings = openSettings,
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
                composable("stats") { StatsScreen(onOpenSettings = openSettings) }
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenGeneral = { navController.navigate("settings/general") },
                        onOpenVehicles = { navController.navigate("settings/vehicles") },
                        onOpenRefuel = { navController.navigate("settings/refuel") },
                        onOpenMaintenance = { navController.navigate("settings/maintenance") },
                        onOpenStats = { navController.navigate("settings/stats") },
                    )
                }
                composable("settings/general") {
                    SettingsGeneralScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/vehicles") {
                    SettingsVehiclesScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/refuel") {
                    SettingsRefuelScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCsvImport = { navController.navigate("settings/refuel/csvImport") },
                    )
                }
                composable("settings/refuel/csvImport") {
                    SettingsRefuelCsvImportScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/maintenance") {
                    SettingsMaintenanceScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/stats") {
                    SettingsStatsScreen(onBack = { navController.popBackStack() })
                }
            }
            LaunchedEffect(pendingAction) {
                if (pendingAction == ACTION_ADD_REFUEL) {
                    navController.navigate("refuelAdd/0")
                    onActionConsumed()
                }
            }
        }
    }
}
