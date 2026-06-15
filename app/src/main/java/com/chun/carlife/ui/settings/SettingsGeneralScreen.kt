package com.chun.carlife.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chun.carlife.CarLifeApp
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsGeneralScreen(onBack: () -> Unit) {
    val db = rememberDatabase()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmOpen by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(title = "一般", onBack = onBack) { padding ->
        SettingsMenuList(
            padding = padding,
            rows = listOf(
                SettingsMenuRow(
                    title = "データ全削除",
                    description = lastMessage
                        ?: "車両・給油・整備・整備スケジュールを含む全データを消去します",
                    onClick = { if (!working) confirmOpen = true },
                ),
            ),
        )
    }

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { if (!working) confirmOpen = false },
            title = { Text("全データを削除しますか?") },
            text = {
                Text(
                    "車両・給油・整備・整備スケジュールのすべての記録が消えます。" +
                        "この操作は取り消せません。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        working = true
                        scope.launch {
                            withContext(Dispatchers.IO) { db.clearAllTables() }
                            SelectedVehicleStore.state.value = null
                            (ctx.applicationContext as CarLifeApp).setDefaultVehicleId(null)
                            lastMessage = "全データを削除しました"
                            working = false
                            confirmOpen = false
                        }
                    },
                ) { Text(if (working) "削除中..." else "削除する") }
            },
            dismissButton = {
                TextButton(
                    enabled = !working,
                    onClick = { confirmOpen = false },
                ) { Text("キャンセル") }
            },
        )
    }
}
