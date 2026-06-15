package com.chun.carlife.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.chun.carlife.domain.FuelEconomy
import com.chun.carlife.ui.util.SelectedVehicleStore
import com.chun.carlife.ui.util.VehiclePicker
import com.chun.carlife.ui.util.formatKm
import com.chun.carlife.ui.util.formatKmpl
import com.chun.carlife.ui.util.formatLiters
import com.chun.carlife.ui.util.formatMoney
import com.chun.carlife.ui.util.monthKey
import com.chun.carlife.ui.util.rememberDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
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
    val refuels by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.refuelDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())
    val maintenances by remember(selected?.id) {
        if (selected == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else db.maintenanceDao().observeByVehicle(selected.id)
    }.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("集計") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vehicles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("先に「車両」タブから車両を登録してください。")
                }
                return@Column
            }
            VehiclePicker(vehicles = vehicles, selected = selected, onSelect = { selectedId = it.id })
            if (selected == null) return@Column

            val asc = refuels.asReversed()
            val summary = remember(asc) { FuelEconomy.summarize(asc) }
            val stats = remember(asc) { FuelEconomy.computeStats(asc) }

            SummaryCard(
                avg = summary.averageKmPerLiter,
                totalKm = summary.totalDistanceKm,
                totalLiters = summary.totalLiters,
                fuelCost = summary.totalCost,
                maintCost = maintenances.sumOf { it.cost },
            )

            val kmplSeries = stats.mapNotNull { it.kmPerLiter }
            if (kmplSeries.size >= 2) {
                ChartCard(title = "燃費推移 (km/L)") {
                    LineChart(values = kmplSeries, color = MaterialTheme.colorScheme.primary)
                }
            }

            val monthly = remember(refuels, maintenances) {
                val months = sortedSetOf<String>()
                val fuel = mutableMapOf<String, Double>()
                val maint = mutableMapOf<String, Double>()
                refuels.forEach {
                    val k = monthKey(it.date); months += k
                    fuel.merge(k, it.totalCost, Double::plus)
                }
                maintenances.forEach {
                    val k = monthKey(it.date); months += k
                    maint.merge(k, it.cost, Double::plus)
                }
                months.toList().takeLast(12).map { k ->
                    Triple(k, fuel[k] ?: 0.0, maint[k] ?: 0.0)
                }
            }
            if (monthly.isNotEmpty()) {
                ChartCard(title = "月別 費用 (¥)") {
                    BarChart(monthly)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(avg: Double?, totalKm: Int, totalLiters: Double, fuelCost: Double, maintCost: Double) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("サマリ", style = MaterialTheme.typography.titleMedium)
            StatRow("平均燃費", formatKmpl(avg))
            StatRow("総走行", formatKm(totalKm))
            StatRow("総給油", formatLiters(totalLiters))
            StatRow("燃料費合計", formatMoney(fuelCost))
            StatRow("整備費合計", formatMoney(maintCost))
            StatRow("総支出", formatMoney(fuelCost + maintCost))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, color: Color) {
    val maxV = values.max()
    val minV = values.min()
    val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val pad = 12f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = if (values.size == 1) w / 2 else pad + (w - 2 * pad) * i / (values.size - 1)
            val y = h - pad - ((v - minV) / range).toFloat() * (h - 2 * pad)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 4f))
        // axis baseline
        drawLine(color = Color.Gray.copy(alpha = 0.4f), start = Offset(0f, h - pad), end = Offset(w, h - pad), strokeWidth = 1f)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatKmpl(minV), style = MaterialTheme.typography.bodySmall)
        Text(formatKmpl(maxV), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BarChart(items: List<Triple<String, Double, Double>>) {
    val maxV = items.maxOf { it.second + it.third }.coerceAtLeast(1.0)
    val fuelColor = MaterialTheme.colorScheme.primary
    val maintColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val pad = 16f
        val barAreaH = h - pad * 2
        val n = items.size
        val gap = 6f
        val barW = ((w - pad * 2) - gap * (n - 1)) / n
        items.forEachIndexed { i, (_, fuel, maint) ->
            val x = pad + i * (barW + gap)
            val fuelH = (fuel / maxV).toFloat() * barAreaH
            val maintH = (maint / maxV).toFloat() * barAreaH
            val baseY = h - pad
            drawRect(color = fuelColor, topLeft = Offset(x, baseY - fuelH), size = androidx.compose.ui.geometry.Size(barW, fuelH))
            drawRect(color = maintColor, topLeft = Offset(x, baseY - fuelH - maintH), size = androidx.compose.ui.geometry.Size(barW, maintH))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(items.first().first, style = MaterialTheme.typography.bodySmall)
        Text(items.last().first, style = MaterialTheme.typography.bodySmall)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(fuelColor); Text("燃料費", style = MaterialTheme.typography.bodySmall)
        LegendDot(maintColor); Text("整備費", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegendDot(color: Color) {
    Canvas(modifier = Modifier.height(12.dp).padding(top = 4.dp)) {
        drawRect(color = color, size = androidx.compose.ui.geometry.Size(12f, 12f))
    }
}

