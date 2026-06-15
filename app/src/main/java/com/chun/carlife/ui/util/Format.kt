package com.chun.carlife.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
private val moneyFmt = NumberFormat.getNumberInstance(Locale.JAPAN)

fun formatDate(epochMillis: Long): String = dateFmt.format(Date(epochMillis))
fun formatMoney(value: Double): String = "¥" + moneyFmt.format(value.toLong())
fun formatLiters(value: Double): String = String.format(Locale.US, "%.2f L", value)
fun formatKmpl(value: Double?): String = value?.let { String.format(Locale.US, "%.2f km/L", it) } ?: "-"
fun formatKm(value: Int): String = moneyFmt.format(value) + " km"

fun parseDouble(text: String): Double? = text.replace(",", "").trim().toDoubleOrNull()
fun parseInt(text: String): Int? = text.replace(",", "").trim().toIntOrNull()

fun monthKey(epochMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}
