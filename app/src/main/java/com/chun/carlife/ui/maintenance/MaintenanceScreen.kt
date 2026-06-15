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
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Maintenance
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.VehiclePicker
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.formatKm
import com.chun.carlife.ui.util.formatMoney
import com.chun.carlife.ui.util.rememberDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(onAdd: (Long) -> Unit, onEdit: (Long, Long) -> Unit) {
    val db = rememberDatabase()
    val vehicles by remember { db.vehicleDao().observeAll() }.collectAsState(initial = emptyList())
    var selectedId by SelectedVehicleStore.state
    LaunchedEffect(vehicles) {
        if (selectedId == null && vehicles.isNotEmpty()) selectedId = vehicles.first().id
        if (selectedId != null && vehicles.none { it.id == selectedId }) {
            selectedId = vehicles.firstOrNull()?.id
        }
    }
    val selected = vehicles.firstOrNull { it.id == selectedId }
    val list by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.maintenanceDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("整備記録") }) },
        floatingActionButton = {
            val id = selected?.id
            if (id != null) {
                FloatingActionButton(onClick = { onAdd(id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "整備を追加")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vehicles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("先に「車両」タブから車両を登録してください。")
                }
                return@Column
            }
            VehiclePicker(vehicles = vehicles, selected = selected, onSelect = { selectedId = it.id })
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("整備記録はまだありません。")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it.id }) { m ->
                        MaintenanceRow(m, onClick = { onEdit(selected!!.id, m.id) })
                    }
                }
            }
        }
    }
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
