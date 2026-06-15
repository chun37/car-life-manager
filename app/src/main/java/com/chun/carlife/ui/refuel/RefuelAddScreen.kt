package com.chun.carlife.ui.refuel

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Refuel
import com.chun.carlife.data.Vehicle
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.formatKm
import com.chun.carlife.ui.util.formatLiters
import com.chun.carlife.ui.util.formatMoney
import com.chun.carlife.ui.util.parseDouble
import com.chun.carlife.ui.util.parseInt
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

private const val STEP_COUNT = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefuelAddScreen(initialVehicleId: Long, onDone: () -> Unit) {
    val db = rememberDatabase()
    val scope = rememberCoroutineScope()
    val vehicles by remember { db.vehicleDao().observeAll() }.collectAsState(initial = emptyList())

    var step by remember { mutableStateOf(0) }
    var selectedVehicleId by SelectedVehicleStore.state
    var odometer by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var fullTank by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(initialVehicleId, vehicles) {
        if (selectedVehicleId == null) {
            selectedVehicleId = if (initialVehicleId != 0L) initialVehicleId else vehicles.firstOrNull()?.id
        }
    }

    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    val canNext: Boolean = when (step) {
        0 -> selectedVehicle != null
        1 -> (parseInt(odometer) ?: 0) > 0
        2 -> (parseDouble(liters) ?: 0.0) > 0.0
        3 -> (parseDouble(totalCost) ?: 0.0) > 0.0
        4 -> true
        else -> false
    }
    val isLast = step == STEP_COUNT - 1

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("給油を追加  ${step + 1}/${STEP_COUNT}") })
                LinearProgressIndicator(
                    progress = { (step + 1) / STEP_COUNT.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { if (step == 0) onDone() else step-- },
                    modifier = Modifier.weight(1f),
                ) { Text(if (step == 0) "キャンセル" else "戻る") }
                Button(
                    onClick = {
                        if (!isLast) {
                            step++
                        } else {
                            val v = selectedVehicle ?: return@Button
                            val odo = parseInt(odometer) ?: return@Button
                            val l = parseDouble(liters) ?: return@Button
                            val total = parseDouble(totalCost) ?: return@Button
                            val unit = if (l > 0) total / l else 0.0
                            val r = Refuel(
                                vehicleId = v.id,
                                date = date,
                                odometer = odo,
                                liters = l,
                                pricePerLiter = unit,
                                totalCost = total,
                                fullTank = fullTank,
                                note = note,
                            )
                            scope.launch {
                                db.refuelDao().upsert(r)
                                onDone()
                            }
                        }
                    },
                    enabled = canNext,
                    modifier = Modifier.weight(1f),
                ) { Text(if (isLast) "保存" else "次へ") }
            }
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
            when (step) {
                0 -> StepVehicle(vehicles, selectedVehicle, onSelect = { selectedVehicleId = it.id })
                1 -> StepOdometer(value = odometer, onChange = { odometer = it })
                2 -> StepLiters(value = liters, onChange = { liters = it })
                3 -> StepTotalCost(value = totalCost, liters = liters, onChange = { totalCost = it })
                4 -> StepConfirm(
                    vehicle = selectedVehicle,
                    date = date,
                    odometer = odometer,
                    liters = liters,
                    totalCost = totalCost,
                    fullTank = fullTank,
                    note = note,
                    onDateChange = { date = it },
                    onFullTankChange = { fullTank = it },
                    onNoteChange = { note = it },
                )
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, hint: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    if (hint != null) {
        Text(hint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepVehicle(vehicles: List<Vehicle>, selected: Vehicle?, onSelect: (Vehicle) -> Unit) {
    StepHeader("どの車両の給油ですか？")
    if (vehicles.isEmpty()) {
        Text("先に「車両」タブから車両を登録してください。")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        vehicles.forEach { v ->
            VehicleSelectCard(
                vehicle = v,
                isSelected = v.id == selected?.id,
                onClick = { onSelect(v) },
            )
        }
    }
}

@Composable
private fun VehicleSelectCard(vehicle: Vehicle, isSelected: Boolean, onClick: () -> Unit) {
    val container = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.name, style = MaterialTheme.typography.titleLarge)
                val sub = listOfNotNull(
                    vehicle.maker.takeIf { it.isNotBlank() },
                    vehicle.model.takeIf { it.isNotBlank() },
                    vehicle.plateNumber.takeIf { it.isNotBlank() },
                ).joinToString(" / ")
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (isSelected) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "選択中")
            }
        }
    }
}

@Composable
private fun StepOdometer(value: String, onChange: (String) -> Unit) {
    StepHeader("走行距離 (ODO) を入力", hint = "現在のメーター表示の km 数")
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text("km") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun StepLiters(value: String, onChange: (String) -> Unit) {
    StepHeader("給油量を入力", hint = "リットル単位")
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text("L") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun StepTotalCost(value: String, liters: String, onChange: (String) -> Unit) {
    StepHeader("合計金額を入力", hint = "支払った金額の合計")
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text("円") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    val l = parseDouble(liters)
    val total = parseDouble(value)
    if (l != null && l > 0 && total != null && total > 0) {
        Text("単価: ${"%.2f".format(total / l)} 円/L", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepConfirm(
    vehicle: Vehicle?,
    date: Long,
    odometer: String,
    liters: String,
    totalCost: String,
    fullTank: Boolean,
    note: String,
    onDateChange: (Long) -> Unit,
    onFullTankChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val ctx = LocalContext.current
    StepHeader("内容を確認")
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryRow("車両", vehicle?.name ?: "-")
            SummaryRow("走行距離", formatKm(parseInt(odometer) ?: 0))
            SummaryRow("給油量", formatLiters(parseDouble(liters) ?: 0.0))
            SummaryRow("合計", formatMoney(parseDouble(totalCost) ?: 0.0))
        }
    }
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            DatePickerDialog(
                ctx,
                { _, y, m, d ->
                    val c = Calendar.getInstance()
                    c.set(y, m, d, 12, 0, 0)
                    onDateChange(c.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("日付: ${formatDate(date)}") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = fullTank, onCheckedChange = onFullTankChange)
        Text("満タン給油（燃費計算の基準になります）")
    }
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("メモ (任意)") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
