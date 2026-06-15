package com.chun.carlife.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.ScheduleOverride
import com.chun.carlife.domain.ScheduleItem
import com.chun.carlife.ui.maintenance.PRESET_CATEGORIES
import com.chun.carlife.ui.maintenance.formatInterval
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.VehiclePicker
import com.chun.carlife.ui.util.parseInt
import com.chun.carlife.ui.util.rememberDatabase
import com.chun.carlife.ui.util.rememberDefaultVehicleId
import com.chun.carlife.ui.util.rememberVehicles
import kotlinx.coroutines.launch

@Composable
fun SettingsMaintenanceScreen(onBack: () -> Unit) {
    val db = rememberDatabase()
    val scope = rememberCoroutineScope()
    val vehiclesOpt by rememberVehicles()
    val defaultId by rememberDefaultVehicleId()
    var selectedId by SelectedVehicleStore.state
    LaunchedEffect(vehiclesOpt, defaultId) {
        val vs = vehiclesOpt ?: return@LaunchedEffect
        SelectedVehicleStore.syncWithDefault(vs, defaultId)
    }
    val selected = vehiclesOpt?.firstOrNull { it.id == selectedId }
    val overrides by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.scheduleOverrideDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())

    var editingItem by remember { mutableStateOf<ScheduleItem?>(null) }
    var isNewSchedule by remember { mutableStateOf(false) }

    SettingsScaffold(title = "整備周期", onBack = onBack) { padding ->
        val vehicles = vehiclesOpt
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                vehicles == null -> Unit
                vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("先に「車両」タブから車両を登録してください。")
                    }
                }
                else -> {
                    VehiclePicker(vehicles = vehicles, selected = selected, onSelect = { selectedId = it.id })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("整備周期", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            editingItem = ScheduleItem("")
                            isNewSchedule = true
                        }) { Text("+ 追加") }
                    }
                    if (overrides.isEmpty()) {
                        Text(
                            "整備周期はまだ登録されていません。",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(overrides, key = { it.id }) { o ->
                                val item = ScheduleItem(
                                    o.category,
                                    intervalKm = o.intervalKm,
                                    intervalMonths = o.intervalMonths,
                                )
                                ScheduleItemRow(
                                    item = item,
                                    onClick = {
                                        editingItem = item
                                        isNewSchedule = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val target = editingItem
    val vehicleId = selected?.id
    if (target != null && vehicleId != null) {
        val close: () -> Unit = { editingItem = null; isNewSchedule = false }
        ScheduleEditDialog(
            item = target,
            isNew = isNewSchedule,
            onDismiss = close,
            onSave = { category, km, months ->
                scope.launch {
                    db.scheduleOverrideDao().upsert(
                        ScheduleOverride(
                            vehicleId = vehicleId,
                            category = category,
                            intervalKm = km,
                            intervalMonths = months,
                        ),
                    )
                    close()
                }
            },
            onDelete = if (isNewSchedule) null else ({
                scope.launch {
                    db.scheduleOverrideDao().deleteByCategory(vehicleId, target.category)
                    close()
                }
            }),
        )
    }
}

@Composable
private fun ScheduleItemRow(item: ScheduleItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.category, style = MaterialTheme.typography.titleSmall)
            Text(formatInterval(item), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditDialog(
    item: ScheduleItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (category: String, intervalKm: Int?, intervalMonths: Int?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var category by remember(item) { mutableStateOf(item.category) }
    var usePreset by remember(item) {
        mutableStateOf(isNew || item.category in PRESET_CATEGORIES)
    }
    var catExpanded by remember(item) { mutableStateOf(false) }
    var kmText by remember(item) { mutableStateOf(item.intervalKm?.toString() ?: "") }
    var monthsText by remember(item) { mutableStateOf(item.intervalMonths?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "整備周期を追加" else "${item.category} の周期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isNew) {
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
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = { category = c; catExpanded = false },
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("種別 *") },
                            placeholder = { Text("自由に入力") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text(
                    "空欄にするとその基準は使われません。両方空欄なら通知なし。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                    label = { Text("距離 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = monthsText,
                    onValueChange = { monthsText = it.filter { c -> c.isDigit() } },
                    label = { Text("月数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = category.isNotBlank(),
                onClick = { onSave(category.trim(), parseInt(kmText), parseInt(monthsText)) },
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("削除") }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )
}
