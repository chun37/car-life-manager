package com.chun.carlife.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.CsvImportResult
import com.chun.carlife.data.RefuelCsvImporter
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val db = rememberDatabase()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<CsvImportResult?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val result = RefuelCsvImporter.import(ctx, db, uri)
            lastResult = result
            importing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("給油データのインポート", style = MaterialTheme.typography.titleMedium)
            Text(
                "CSV ファイルを選択して給油記録をまとめて追加します。" +
                    "1 行目にヘッダ、車両は登録済みの「名前」と一致させてください。",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CSV フォーマット", style = MaterialTheme.typography.titleSmall)
                    Text("必須: " + RefuelCsvImporter.ALL_HEADERS.take(5).joinToString("、"))
                    Text("任意: " + RefuelCsvImporter.ALL_HEADERS.drop(5).joinToString("、"))
                    Text(
                        text = """
                            車両,日付,走行距離,給油量,単価,合計金額,満タン,メモ
                            プリウス,2026/06/15,55000,30.5,180,5500,true,セルフ
                            プリウス,2026/05/20,54000,28.0,175,4900,true,
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                    Text("日付は yyyy/MM/dd, yyyy-MM-dd, yyyy.MM.dd のいずれか。満タンは true/false。")
                }
            }

            Button(
                onClick = {
                    picker.launch(
                        arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "text/plain",
                            "application/csv",
                            "application/vnd.ms-excel",
                            "application/octet-stream",
                            "*/*",
                        )
                    )
                },
                enabled = !importing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (importing) "インポート中..." else "CSV ファイルを選択") }

            val result = lastResult
            if (result != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.skipped > 0) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("結果: ${result.added} 件追加 / ${result.skipped} 件スキップ", style = MaterialTheme.typography.titleSmall)
                        result.errors.take(20).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (result.errors.size > 20) {
                            Text("…他 ${result.errors.size - 20} 件", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
