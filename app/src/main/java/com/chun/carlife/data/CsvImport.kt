package com.chun.carlife.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CsvImportResult(
    val added: Int,
    val skipped: Int,
    val errors: List<String>,
)

object RefuelCsvImporter {
    private val DATE_PATTERNS = listOf("yyyy/MM/dd", "yyyy-MM-dd", "yyyy.MM.dd")

    // 量・単価・満タンは EV (充電量 / kWh単価 / 満充電) と FUEL (給油量 / 単価 / 満タン) を
    // 同じ意味として受け入れる。CSV は「給油量 か 充電量」のどちらかが必須。
    private val AMOUNT_ALIASES = listOf("給油量", "充電量")
    private val UNIT_PRICE_ALIASES = listOf("単価", "kWh単価")
    private val FULL_TANK_ALIASES = listOf("満タン", "満充電")

    private val FIXED_REQUIRED_HEADERS = listOf("車両", "日付", "走行距離", "合計金額")
    private val OPTIONAL_HEADERS = UNIT_PRICE_ALIASES + FULL_TANK_ALIASES + listOf("メモ")
    val ALL_HEADERS = FIXED_REQUIRED_HEADERS + AMOUNT_ALIASES + OPTIONAL_HEADERS

    suspend fun import(ctx: Context, db: AppDatabase, uri: Uri): CsvImportResult =
        withContext(Dispatchers.IO) {
            val text = ctx.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext CsvImportResult(0, 0, listOf("ファイルを開けませんでした"))

            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return@withContext CsvImportResult(0, 0, listOf("ファイルが空です"))

            val header = parseCsvLine(lines[0]).map { it.trim() }
            val missingFixed = FIXED_REQUIRED_HEADERS.filter { it !in header }
            val hasAmount = AMOUNT_ALIASES.any { it in header }
            if (missingFixed.isNotEmpty() || !hasAmount) {
                val missing = missingFixed + if (!hasAmount) listOf(AMOUNT_ALIASES.joinToString(" / ")) else emptyList()
                return@withContext CsvImportResult(
                    0, 0,
                    listOf("必須カラム不足: " + missing.joinToString("、")),
                )
            }
            val idx = ALL_HEADERS.associateWith { header.indexOf(it) }
            fun firstColumn(aliases: List<String>): Int =
                aliases.map { idx[it] ?: -1 }.firstOrNull { it >= 0 } ?: -1
            val amountCol = firstColumn(AMOUNT_ALIASES)
            val unitPriceCol = firstColumn(UNIT_PRICE_ALIASES)
            val fullTankCol = firstColumn(FULL_TANK_ALIASES)

            val vehicles = db.vehicleDao().getAll()
            val vehicleByName = vehicles.associateBy { it.name }

            var added = 0
            var skipped = 0
            val errors = mutableListOf<String>()

            for ((i, line) in lines.drop(1).withIndex()) {
                val rowNum = i + 2
                val cols = parseCsvLine(line)
                try {
                    val name = cols.getOrNull(idx["車両"]!!)?.trim().orEmpty()
                    val vehicle = vehicleByName[name]
                    if (vehicle == null) {
                        skipped++
                        errors += "${rowNum}行目: 車両「$name」が未登録"
                        continue
                    }
                    val date = parseDate(cols[idx["日付"]!!].trim())
                    val odo = cols[idx["走行距離"]!!].trim().toInt()
                    val liters = cols[amountCol].trim().toDouble()
                    val total = cols[idx["合計金額"]!!].trim().toDouble()

                    val unit = if (unitPriceCol >= 0) cols.getOrNull(unitPriceCol)?.trim()?.toDoubleOrNull() else null
                    val ppl = unit ?: if (liters > 0) total / liters else 0.0
                    val fullTank = if (fullTankCol >= 0) cols.getOrNull(fullTankCol)?.trim()?.let(::parseBool) ?: true else true
                    val note = idx["メモ"]?.let { ix ->
                        if (ix >= 0) cols.getOrNull(ix).orEmpty() else ""
                    } ?: ""

                    db.refuelDao().upsert(
                        Refuel(
                            vehicleId = vehicle.id,
                            date = date,
                            odometer = odo,
                            liters = liters,
                            pricePerLiter = ppl,
                            totalCost = total,
                            fullTank = fullTank,
                            note = note,
                        )
                    )
                    added++
                } catch (e: Throwable) {
                    skipped++
                    errors += "${rowNum}行目: ${e.message ?: "不明なエラー"}"
                }
            }
            CsvImportResult(added, skipped, errors)
        }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    out += sb.toString()
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    private fun parseDate(s: String): Long {
        for (p in DATE_PATTERNS) {
            try {
                val fmt = SimpleDateFormat(p, Locale.JAPAN).apply { isLenient = false }
                val d = fmt.parse(s) ?: continue
                val cal = Calendar.getInstance().apply {
                    time = d
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } catch (_: Throwable) {
            }
        }
        error("日付形式が不正: $s")
    }

    private fun parseBool(s: String): Boolean {
        val v = s.trim().lowercase()
        return v in setOf("true", "1", "yes", "y", "○", "◯", "満タン", "満")
    }
}
