package com.chun.carlife.ui.refuel

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Refuel
import com.chun.carlife.data.Vehicle
import com.chun.carlife.domain.FuelEconomy
import com.chun.carlife.domain.RefuelStat
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.VehiclePicker
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.formatKm
import com.chun.carlife.ui.util.formatKmpl
import com.chun.carlife.ui.util.formatLiters
import com.chun.carlife.ui.util.formatMoney
import com.chun.carlife.ui.util.rememberDatabase
import com.chun.carlife.ui.util.rememberDefaultVehicleId
import com.chun.carlife.ui.util.rememberVehicles
import com.chun.carlife.ui.util.resolveInitialVehicleId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefuelScreen(onAdd: (Long) -> Unit, onEdit: (Long, Long) -> Unit) {
    val vehiclesOpt by rememberVehicles()
    val defaultId by rememberDefaultVehicleId()
    var selectedId by SelectedVehicleStore.state

    LaunchedEffect(vehiclesOpt, defaultId) {
        val vs = vehiclesOpt ?: return@LaunchedEffect
        selectedId = resolveInitialVehicleId(selectedId, vs, defaultId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("給油・燃費") }) },
        floatingActionButton = {
            val id = selectedId
            if (id != null && vehiclesOpt?.isNotEmpty() == true) {
                FloatingActionButton(onClick = { onAdd(id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "給油を追加")
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
                    val selected = vehicles.firstOrNull { it.id == selectedId }
                    VehiclePicker(
                        vehicles = vehicles,
                        selected = selected,
                        onSelect = { selectedId = it.id },
                    )
                    if (selected != null) RefuelBody(vehicle = selected, onEdit = onEdit)
                }
            }
        }
    }
}

@Composable
private fun RefuelBody(vehicle: Vehicle, onEdit: (Long, Long) -> Unit) {
    val db = rememberDatabase()
    val refuels by remember(vehicle.id) {
        db.refuelDao().observeByVehicle(vehicle.id)
    }.collectAsState(initial = emptyList())

    val asc = refuels.asReversed()
    val stats = remember(asc) { FuelEconomy.computeStats(asc) }
    val summary = remember(asc) { FuelEconomy.summarize(asc) }

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("平均燃費")
                Text(formatKmpl(summary.averageKmPerLiter), style = MaterialTheme.typography.titleSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("総走行")
                Text(formatKm(summary.totalDistanceKm))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("総給油")
                Text(formatLiters(summary.totalLiters))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("総額")
                Text(formatMoney(summary.totalCost))
            }
        }
    }

    if (refuels.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("給油記録はまだありません。")
        }
    } else {
        // 表示は新しい順 (= refuels)。stats は古い順なので逆引きマップで紐づける。
        val statByRefuelId = stats.associateBy { it.refuel.id }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(refuels, key = { it.id }) { r ->
                RefuelRow(
                    refuel = r,
                    stat = statByRefuelId[r.id],
                    onClick = { onEdit(vehicle.id, r.id) },
                )
            }
        }
    }
}

@Composable
private fun RefuelRow(refuel: Refuel, stat: RefuelStat?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(refuel.date), style = MaterialTheme.typography.titleSmall)
                Text(formatKmpl(stat?.kmPerLiter), style = MaterialTheme.typography.titleSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatKm(refuel.odometer))
                Text(formatLiters(refuel.liters))
                Text(formatMoney(refuel.totalCost))
            }
            if (!refuel.fullTank) {
                Text("満タンではない", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
