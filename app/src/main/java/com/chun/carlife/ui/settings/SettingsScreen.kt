package com.chun.carlife.ui.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenRefuel: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenStats: () -> Unit,
) {
    SettingsScaffold(title = "設定", onBack = onBack) { padding ->
        SettingsMenuList(
            padding = padding,
            rows = listOf(
                SettingsMenuRow(
                    title = "一般",
                    description = "アプリ全体に関わる設定",
                    onClick = onOpenGeneral,
                ),
                SettingsMenuRow(
                    title = "車両",
                    description = "車両タブに関する設定",
                    onClick = onOpenVehicles,
                ),
                SettingsMenuRow(
                    title = "給油",
                    description = "給油タブに関する設定",
                    onClick = onOpenRefuel,
                ),
                SettingsMenuRow(
                    title = "整備",
                    description = "整備タブに関する設定",
                    onClick = onOpenMaintenance,
                ),
                SettingsMenuRow(
                    title = "集計",
                    description = "集計タブに関する設定",
                    onClick = onOpenStats,
                ),
            ),
        )
    }
}
