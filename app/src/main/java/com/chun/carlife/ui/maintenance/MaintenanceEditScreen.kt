package com.chun.carlife.ui.maintenance

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Maintenance
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.parseDouble
import com.chun.carlife.ui.util.parseInt
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceEditScreen(vehicleId: Long, maintenanceId: Long, onDone: () -> Unit) {
    val db = rememberDatabase()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometer by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var existing by remember { mutableStateOf<Maintenance?>(null) }
    var loaded by remember { mutableStateOf(maintenanceId == 0L) }
    var usePreset by remember { mutableStateOf(true) }
    var catExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(maintenanceId) {
        if (maintenanceId != 0L) {
            db.maintenanceDao().observeByVehicle(vehicleId).collect { all ->
                val m = all.firstOrNull { it.id == maintenanceId }
                existing = m
                if (m != null && !loaded) {
                    date = m.date
                    odometer = m.odometer.toString()
                    category = m.category
                    usePreset = m.category in PRESET_CATEGORIES || m.category.isEmpty()
                    cost = m.cost.toString()
                    note = m.note
                }
                loaded = true
                return@collect
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (maintenanceId == 0L) "整備を追加" else "整備を編集") }) },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d, 12, 0, 0)
                            date = c.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("日付: ${formatDate(date)}") }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = usePreset,
                    onClick = {
                        if (!usePreset) {
                            usePreset = true
                            if (category !in PRESET_CATEGORIES) category = ""
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("プリセット") }
                SegmentedButton(
                    selected = !usePreset,
                    onClick = { usePreset = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("自由入力") }
            }

            if (usePreset) {
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded },
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = category,
                        onValueChange = {},
                        label = { Text("種別 *") },
                        placeholder = { Text("選択してください") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false },
                    ) {
                        PRESET_CATEGORIES.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("種別 *") },
                    placeholder = { Text("自由に入力") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it.filter { c -> c.isDigit() } },
                label = { Text("走行距離 (km) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("費用 (円)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("メモ") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("キャンセル") }
                Button(
                    onClick = {
                        val odo = parseInt(odometer) ?: return@Button
                        val cat = category.trim()
                        if (cat.isEmpty()) return@Button
                        val c = parseDouble(cost) ?: 0.0
                        val m = (existing ?: Maintenance(
                            vehicleId = vehicleId,
                            date = date,
                            odometer = odo,
                            category = cat,
                            cost = c,
                            note = note,
                        )).copy(
                            date = date,
                            odometer = odo,
                            category = cat,
                            cost = c,
                            note = note,
                        )
                        scope.launch {
                            db.maintenanceDao().upsert(m)
                            onDone()
                        }
                    },
                    enabled = parseInt(odometer) != null && category.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            db.maintenanceDao().delete(existing!!)
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("この記録を削除") }
            }
        }
    }
}
