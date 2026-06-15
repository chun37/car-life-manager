package com.chun.carlife.ui.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsVehiclesScreen(onBack: () -> Unit) {
    SettingsScaffold(title = "車両", onBack = onBack) { padding ->
        SettingsEmptyContent(padding)
    }
}

@Composable
fun SettingsStatsScreen(onBack: () -> Unit) {
    SettingsScaffold(title = "集計", onBack = onBack) { padding ->
        SettingsEmptyContent(padding)
    }
}
