package com.chun.carlife.ui.util

import com.chun.carlife.domain.EnergyKind
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
private val moneyFmt = NumberFormat.getNumberInstance(Locale.JAPAN)

fun formatDate(epochMillis: Long): String = dateFmt.format(Date(epochMillis))
fun formatMoney(value: Double): String = "¥" + moneyFmt.format(value.toLong())

fun formatAmount(value: Double, kind: EnergyKind = EnergyKind.FUEL): String =
    String.format(Locale.US, "%.2f %s", value, kind.labels().amountUnit)

fun formatEfficiency(value: Double?, kind: EnergyKind = EnergyKind.FUEL): String =
    value?.let { String.format(Locale.US, "%.2f %s", it, kind.labels().efficiencyUnit) } ?: "-"

fun formatKm(value: Int): String = moneyFmt.format(value) + " km"

fun parseDouble(text: String): Double? = text.replace(",", "").trim().toDoubleOrNull()
fun parseInt(text: String): Int? = text.replace(",", "").trim().toIntOrNull()

fun monthKey(epochMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun formatRemainingKm(kmLeft: Int): String = when {
    kmLeft >= 0 -> "あと " + moneyFmt.format(kmLeft) + " km"
    else -> moneyFmt.format(-kmLeft) + " km 超過"
}

fun formatRemainingDays(daysLeft: Int): String = when {
    daysLeft >= 30 -> "あと " + (daysLeft / 30) + " ヶ月"
    daysLeft >= 0 -> "あと " + daysLeft + " 日"
    daysLeft > -30 -> (-daysLeft).toString() + " 日超過"
    else -> ((-daysLeft) / 30).toString() + " ヶ月超過"
}
