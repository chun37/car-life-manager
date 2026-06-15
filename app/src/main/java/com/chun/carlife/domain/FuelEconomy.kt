package com.chun.carlife.domain

import com.chun.carlife.data.Refuel

data class RefuelStat(
    val refuel: Refuel,
    val kmPerLiter: Double?,
    val distanceKm: Int?,
)

data class Summary(
    val totalDistanceKm: Int,
    val totalLiters: Double,
    val totalCost: Double,
    val averageKmPerLiter: Double?,
)

object FuelEconomy {
    /** 同一日付内ではオドメーター昇順 → id 昇順で並べる（物理的な順序を優先）。 */
    private fun canonicalize(refuels: List<Refuel>): List<Refuel> =
        refuels.sortedWith(compareBy({ it.date }, { it.odometer }, { it.id }))

    /**
     * 満タン法による燃費。すべての給油は物理的に満タンに入れている前提で、
     * (今回ODO − 前回ODO) / 今回の給油量 で各区間の km/L を出す。
     * `fullTank=false` は「この給油より前の区間に記録忘れがあるかも」を示すマーカーで、
     * その行の kmPerLiter は null になる (区間の信頼性が低い)。
     * ただし次の区間の起点としてはそのまま使う。
     * 並び順は内部で正規化するため呼び出し側の順序に依存しない。
     */
    fun computeStats(refuelsAsc: List<Refuel>): List<RefuelStat> {
        if (refuelsAsc.isEmpty()) return emptyList()
        val sorted = canonicalize(refuelsAsc)
        return sorted.mapIndexed { i, r ->
            val prev = if (i == 0) null else sorted[i - 1]
            val distance = prev?.let { (r.odometer - it.odometer).takeIf { d -> d > 0 } }
            val kmpl = if (r.fullTank && distance != null && r.liters > 0) {
                distance.toDouble() / r.liters
            } else null
            RefuelStat(r, kmpl, distance)
        }
    }

    fun summarize(refuelsAsc: List<Refuel>): Summary {
        if (refuelsAsc.isEmpty()) return Summary(0, 0.0, 0.0, null)
        val sorted = canonicalize(refuelsAsc)
        val totalLiters = sorted.sumOf { it.liters }
        val totalCost = sorted.sumOf { it.totalCost }
        val totalDistance = (sorted.last().odometer - sorted.first().odometer).coerceAtLeast(0)
        // 平均燃費は記録忘れ疑いを含まない区間 (= fullTank=true な給油の区間) だけで集計
        var sumDistance = 0
        var sumLiters = 0.0
        for (i in 1 until sorted.size) {
            val r = sorted[i]
            if (!r.fullTank) continue
            val d = r.odometer - sorted[i - 1].odometer
            if (d > 0 && r.liters > 0) {
                sumDistance += d
                sumLiters += r.liters
            }
        }
        val avg = if (sumDistance > 0 && sumLiters > 0) sumDistance / sumLiters else null
        return Summary(totalDistance, totalLiters, totalCost, avg)
    }
}
