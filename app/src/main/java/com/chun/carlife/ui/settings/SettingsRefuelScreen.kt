package com.chun.carlife.ui.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsRefuelScreen(
    onBack: () -> Unit,
    onOpenCsvImport: () -> Unit,
) {
    SettingsScaffold(title = "給油", onBack = onBack) { padding ->
        SettingsMenuList(
            padding = padding,
            rows = listOf(
                SettingsMenuRow(
                    title = "CSV インポート",
                    description = "CSV ファイルから給油記録を一括追加",
                    onClick = onOpenCsvImport,
                ),
            ),
        )
    }
}
