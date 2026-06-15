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
    /**
     * 満タン法による燃費。満タン→満タンの間の距離 ÷ その間の給油量。
     * 入力は古い順。先頭は基準として常に kmPerLiter=null。
     */
    fun computeStats(refuelsAsc: List<Refuel>): List<RefuelStat> {
        if (refuelsAsc.isEmpty()) return emptyList()
        val result = mutableListOf<RefuelStat>()
        var lastFullIndex: Int? = null
        var litersAccum = 0.0

        for ((i, r) in refuelsAsc.withIndex()) {
            if (i == 0) {
                result += RefuelStat(r, null, null)
                if (r.fullTank) lastFullIndex = i
                continue
            }
            val prev = refuelsAsc[i - 1]
            val distance = (r.odometer - prev.odometer).takeIf { it > 0 }

            if (r.fullTank && lastFullIndex != null) {
                val baseIndex = lastFullIndex!!
                val totalDistance = r.odometer - refuelsAsc[baseIndex].odometer
                litersAccum += r.liters
                val kmpl = if (totalDistance > 0 && litersAccum > 0) totalDistance / litersAccum else null
                result += RefuelStat(r, kmpl, distance)
                lastFullIndex = i
                litersAccum = 0.0
            } else {
                if (lastFullIndex != null) litersAccum += r.liters
                result += RefuelStat(r, null, distance)
                if (r.fullTank) {
                    lastFullIndex = i
                    litersAccum = 0.0
                }
            }
        }
        return result
    }

    fun summarize(refuelsAsc: List<Refuel>): Summary {
        if (refuelsAsc.isEmpty()) return Summary(0, 0.0, 0.0, null)
        val totalLiters = refuelsAsc.sumOf { it.liters }
        val totalCost = refuelsAsc.sumOf { it.totalCost }
        val totalDistance = (refuelsAsc.last().odometer - refuelsAsc.first().odometer).coerceAtLeast(0)
        // 平均は最初の給油量を分母から除外（最初の給油は基準点）
        val litersForAverage = totalLiters - refuelsAsc.first().liters
        val avg = if (totalDistance > 0 && litersForAverage > 0) totalDistance / litersForAverage else null
        return Summary(totalDistance, totalLiters, totalCost, avg)
    }
}
