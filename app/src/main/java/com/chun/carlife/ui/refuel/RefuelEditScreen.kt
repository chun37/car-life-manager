package com.chun.carlife.ui.refuel

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chun.carlife.data.Refuel
import com.chun.carlife.domain.EnergyKind
import com.chun.carlife.domain.energy
import com.chun.carlife.ui.util.formatDate
import com.chun.carlife.ui.util.labels
import com.chun.carlife.ui.util.parseDouble
import com.chun.carlife.ui.util.parseInt
import com.chun.carlife.ui.util.rememberDatabase
import kotlinx.coroutines.launch
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefuelEditScreen(vehicleId: Long, refuelId: Long, onDone: () -> Unit) {
    val db = rememberDatabase()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometer by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var fullTank by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var existing by remember { mutableStateOf<Refuel?>(null) }
    var loaded by remember { mutableStateOf(refuelId == 0L) }
    var vehicleKind by remember { mutableStateOf(EnergyKind.FUEL) }

    LaunchedEffect(vehicleId) {
        vehicleKind = db.vehicleDao().getById(vehicleId)?.energy ?: EnergyKind.FUEL
    }

    LaunchedEffect(refuelId) {
        if (refuelId != 0L) {
            val all = db.refuelDao().listByVehicleAsc(vehicleId)
            val r = all.firstOrNull { it.id == refuelId }
            existing = r
            if (r != null) {
                date = r.date
                odometer = r.odometer.toString()
                liters = r.liters.toString()
                pricePerLiter = r.pricePerLiter.toString()
                totalCost = r.totalCost.toString()
                fullTank = r.fullTank
                note = r.note
            }
            loaded = true
        }
    }

    val labels = vehicleKind.labels()
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (refuelId == 0L) labels.addTitle else labels.editTitle) }) },
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

            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it.filter { c -> c.isDigit() } },
                label = { Text("走行距離 (km) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = liters,
                onValueChange = {
                    liters = it.filter { c -> c.isDigit() || c == '.' }
                    val l = parseDouble(liters)
                    val p = parseDouble(pricePerLiter)
                    if (l != null && p != null) totalCost = String.format("%.0f", l * p)
                },
                label = { Text("${labels.amountLabel} (${labels.amountUnit}) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pricePerLiter,
                onValueChange = {
                    pricePerLiter = it.filter { c -> c.isDigit() || c == '.' }
                    val l = parseDouble(liters)
                    val p = parseDouble(pricePerLiter)
                    if (l != null && p != null) totalCost = String.format("%.0f", l * p)
                },
                label = { Text(labels.unitPriceLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = totalCost,
                onValueChange = { totalCost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("合計金額 (円) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = fullTank, onCheckedChange = { fullTank = it })
                Text(labels.fullTankLabel)
            }
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
                        val l = parseDouble(liters) ?: return@Button
                        val total = parseDouble(totalCost) ?: return@Button
                        val unit = parseDouble(pricePerLiter) ?: if (l > 0) total / l else 0.0
                        val r = (existing ?: Refuel(
                            vehicleId = vehicleId,
                            date = date,
                            odometer = odo,
                            liters = l,
                            pricePerLiter = unit,
                            totalCost = total,
                            fullTank = fullTank,
                            note = note,
                        )).copy(
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
                    },
                    enabled = parseInt(odometer) != null && parseDouble(liters) != null && parseDouble(totalCost) != null,
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            db.refuelDao().delete(existing!!)
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("この記録を削除") }
            }
        }
    }
}
