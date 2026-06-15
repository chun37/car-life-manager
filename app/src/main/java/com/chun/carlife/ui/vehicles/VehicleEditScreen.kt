package com.chun.carlife.ui.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Vehicle
import com.chun.carlife.ui.util.parseDouble
import com.chun.carlife.ui.util.parseInt
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(vehicleId: Long, onDone: () -> Unit) {
    val db = rememberDatabase()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var maker by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var tank by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(vehicleId == 0L) }
    var existing by remember { mutableStateOf<Vehicle?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleId) {
        if (vehicleId != 0L) {
            val v = db.vehicleDao().getById(vehicleId)
            existing = v
            if (v != null) {
                name = v.name
                maker = v.maker
                model = v.model
                plate = v.plateNumber
                odometer = if (v.initialOdometer > 0) v.initialOdometer.toString() else ""
                tank = v.fuelTankCapacityLiters?.toString() ?: ""
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (vehicleId == 0L) "車両を追加" else "車両を編集") }) },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名前 *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = maker, onValueChange = { maker = it }, label = { Text("メーカー") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("型式・モデル") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("ナンバー") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it.filter { c -> c.isDigit() } },
                label = { Text("初期走行距離 (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tank,
                onValueChange = { tank = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("タンク容量 (L) 任意") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("キャンセル") }
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val v = (existing ?: Vehicle(name = name)).copy(
                            name = name.trim(),
                            maker = maker.trim(),
                            model = model.trim(),
                            plateNumber = plate.trim(),
                            initialOdometer = parseInt(odometer) ?: 0,
                            fuelTankCapacityLiters = parseDouble(tank),
                        )
                        scope.launch {
                            if (v.id == 0L) db.vehicleDao().upsert(v) else db.vehicleDao().update(v)
                            onDone()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("この車両を削除") }
            }
        }
    }

    if (showDeleteConfirm && existing != null) {
        val target = existing!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("車両を削除しますか?") },
            text = {
                Text("「${target.name}」と紐づく給油・整備の記録もすべて削除されます。この操作は取り消せません。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        db.vehicleDao().delete(target)
                        onDone()
                    }
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}
