package com.chun.carlife.ui.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Vehicle
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val db = rememberDatabase()
    val vehiclesFlow: Flow<List<Vehicle>> = remember { db.vehicleDao().observeAll() }
    val vehicles by vehiclesFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("車両") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "追加")
            }
        },
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("まだ車両がありません。右下の + から登録してください。")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vehicles, key = { it.id }) { v -> VehicleRow(v, onClick = { onEdit(v.id) }) }
            }
        }
    }
}

@Composable
private fun VehicleRow(v: Vehicle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(v.name, style = MaterialTheme.typography.titleMedium)
            val sub = listOfNotNull(
                v.maker.takeIf { it.isNotBlank() },
                v.model.takeIf { it.isNotBlank() },
                v.plateNumber.takeIf { it.isNotBlank() },
            ).joinToString(" / ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
