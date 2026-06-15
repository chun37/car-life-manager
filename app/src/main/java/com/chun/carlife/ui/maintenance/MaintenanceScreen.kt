package com.chun.carlife.ui.maintenance

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Maintenance
import com.chun.carlife.domain.MaintenanceSchedule
import com.chun.carlife.domain.ScheduleItem
import com.chun.carlife.domain.ScheduleStatus
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.SettingsAction
import com.chun.carlife.ui.util.VehiclePicker
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.formatKm
import com.chun.carlife.ui.util.formatMoney
import com.chun.carlife.ui.util.formatRemainingDays
import com.chun.carlife.ui.util.formatRemainingKm
import com.chun.carlife.ui.util.rememberDatabase
import com.chun.carlife.ui.util.rememberDefaultVehicleId
import com.chun.carlife.ui.util.rememberVehicles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(onAdd: (Long) -> Unit, onEdit: (Long, Long) -> Unit, onOpenSettings: () -> Unit) {
    val db = rememberDatabase()
    val vehiclesOpt by rememberVehicles()
    val defaultId by rememberDefaultVehicleId()
    var selectedId by SelectedVehicleStore.state
    LaunchedEffect(vehiclesOpt, defaultId) {
        val vs = vehiclesOpt ?: return@LaunchedEffect
        SelectedVehicleStore.syncWithDefault(vs, defaultId)
    }
    val selected = vehiclesOpt?.firstOrNull { it.id == selectedId }
    val list by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.maintenanceDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())
    val refuels by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.refuelDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())
    val overrides by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.scheduleOverrideDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("整備記録") },
                actions = { SettingsAction(onClick = onOpenSettings) },
            )
        },
        floatingActionButton = {
            val id = selected?.id
            if (id != null) {
                FloatingActionButton(onClick = { onAdd(id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "整備を追加")
                }
            }
        },
    ) { padding ->
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
                    val currentOdo = maxOf(
                        selected?.initialOdometer ?: 0,
                        refuels.maxOfOrNull { it.odometer } ?: 0,
                        list.maxOfOrNull { it.odometer } ?: 0,
                    )
                    val schedule = remember(overrides) {
                        overrides.map {
                            ScheduleItem(it.category, intervalKm = it.intervalKm, intervalMonths = it.intervalMonths)
                        }
                    }
                    val statuses = remember(list, currentOdo, schedule) {
                        MaintenanceSchedule.computeStatuses(list, currentOdo, schedule = schedule)
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item("schedule-header") {
                            SectionHeader("メンテナンス予定")
                        }
                        if (statuses.isEmpty()) {
                            item("schedule-empty") {
                                Text(
                                    "整備周期はまだ登録されていません。設定 → 整備 から追加してください。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        } else {
                            items(statuses, key = { "sched-${it.item.category}" }) { s ->
                                ScheduleCard(s)
                            }
                        }
                        item("records-header") {
                            SectionHeader("履歴", topPadding = 12.dp)
                        }
                        if (list.isEmpty()) {
                            item("records-empty") {
                                Text(
                                    "整備記録はまだありません。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        } else {
                            items(list, key = { "rec-${it.id}" }) { m ->
                                MaintenanceRow(m, onClick = { onEdit(selected!!.id, m.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

@Composable
private fun ScheduleCard(s: ScheduleStatus) {
    val containerColor = when {
        s.isOverdue -> MaterialTheme.colorScheme.errorContainer
        s.isSoon -> Color(0xFFFFF4E5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        s.isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.item.category, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (!s.hasHistory) "履歴なし" else if (s.isOverdue) "要交換" else if (s.isSoon) "もうすぐ" else "OK",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            val intervalText = formatInterval(s.item)
            if (!s.hasHistory) {
                Text("$intervalText ごと", style = MaterialTheme.typography.bodyMedium)
            } else {
                val parts = listOfNotNull(
                    s.kmLeft?.let { formatRemainingKm(it) },
                    s.daysLeft?.let { formatRemainingDays(it) },
                )
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts.joinToString(" / "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = "前回: ${formatDate(s.lastDate!!)}  ${formatKm(s.lastOdometer ?: 0)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "周期: $intervalText",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun formatInterval(item: ScheduleItem): String {
    val parts = buildList {
        item.intervalKm?.let { add("${it / 1000}千km") }
        item.intervalMonths?.let { add("${it}ヶ月") }
    }
    return if (parts.isEmpty()) "未設定" else parts.joinToString(" / ")
}

@Composable
private fun MaintenanceRow(m: Maintenance, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.category, style = MaterialTheme.typography.titleSmall)
                Text(formatMoney(m.cost), style = MaterialTheme.typography.titleSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(m.date))
                Text(formatKm(m.odometer))
            }
            if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall)
        }
    }
}
