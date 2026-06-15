package com.chun.carlife.ui.vehicles

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Vehicle
import com.chun.carlife.ui.util.SettingsAction
import com.chun.carlife.ui.util.rememberDefaultVehicleId
import com.chun.carlife.ui.util.rememberSetDefaultVehicleId
import com.chun.carlife.ui.util.rememberVehicles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(onAdd: () -> Unit, onEdit: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val vehiclesOpt by rememberVehicles()
    val defaultId by rememberDefaultVehicleId()
    val setDefault = rememberSetDefaultVehicleId()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("車両") },
                actions = { SettingsAction(onClick = onOpenSettings) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "追加")
            }
        },
    ) { padding ->
        val vehicles = vehiclesOpt
        when {
            vehicles == null -> Unit
            vehicles.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("まだ車両がありません。右下の + から登録してください。")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vehicles, key = { it.id }) { v ->
                        VehicleRow(
                            v = v,
                            isDefault = v.id == defaultId,
                            onClick = { onEdit(v.id) },
                            onToggleDefault = {
                                setDefault(if (v.id == defaultId) null else v.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(
    v: Vehicle,
    isDefault: Boolean,
    onClick: () -> Unit,
    onToggleDefault: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .padding(16.dp),
            ) {
                Text(v.name, style = MaterialTheme.typography.titleMedium)
                val sub = listOfNotNull(
                    v.maker.takeIf { it.isNotBlank() },
                    v.model.takeIf { it.isNotBlank() },
                    v.plateNumber.takeIf { it.isNotBlank() },
                ).joinToString(" / ")
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium)
                }
                if (isDefault) {
                    Text(
                        "デフォルト",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onToggleDefault, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    imageVector = if (isDefault) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isDefault) "デフォルトを解除" else "デフォルトに設定",
                    tint = if (isDefault) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.5f),
                )
            }
        }
    }
}
