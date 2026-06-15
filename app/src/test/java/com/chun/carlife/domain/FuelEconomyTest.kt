package com.chun.carlife.domain

import com.chun.carlife.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelEconomyTest {
    private fun r(id: Long, date: Long, odom: Int, liters: Double, full: Boolean): Refuel =
        Refuel(
            id = id,
            vehicleId = 1L,
            date = date,
            odometer = odom,
            liters = liters,
            pricePerLiter = 0.0,
            totalCost = 0.0,
            fullTank = full,
        )

    @Test
    fun first_record_has_no_kmpl() {
        val r0 = r(100, 0, 62575, 17.60, true)
        val stats = FuelEconomy.computeStats(listOf(r0))
        assertNull(stats[0].kmPerLiter)
    }

    @Test
    fun consecutive_fulls_use_previous_odo_and_current_liters() {
        // ユーザー報告の状況: 全部 true なら各行で (今回ODO - 前回ODO) / 今回の給油量
        val r1 = r(101, 1, 62724, 17.60, true) // 先頭
        val r2 = r(102, 2, 63198, 27.36, true)
        val r3 = r(103, 3, 63292, 15.03, true)
        val stats = FuelEconomy.computeStats(listOf(r1, r2, r3))

        assertNull("先頭は基準なので null", stats[0].kmPerLiter)
        assertEquals(474.0 / 27.36, stats[1].kmPerLiter!!, 0.001) // 17.324
        assertEquals(94.0 / 15.03, stats[2].kmPerLiter!!, 0.001)  // 6.254
    }

    @Test
    fun missing_record_marker_nulls_only_that_row() {
        // 01/19 を false (= 前の区間に記録忘れ疑いあり) にしても、
        // - 01/19 の kmpl は null
        // - 01/23 (満タン) は 01/19 を起点にそのまま計算できる
        val r1 = r(101, 1, 62724, 17.60, true)
        val r2 = r(102, 2, 63198, 27.36, false) // 記録忘れマーカー
        val r3 = r(103, 3, 63292, 15.03, true)
        val stats = FuelEconomy.computeStats(listOf(r1, r2, r3))

        assertNull(stats[0].kmPerLiter)
        assertNull("記録忘れ疑いの区間は kmpl=null", stats[1].kmPerLiter)
        assertEquals("次の区間は 01/19 起点で計算できる", 94.0 / 15.03, stats[2].kmPerLiter!!, 0.001)
    }

    @Test
    fun summarize_excludes_intervals_marked_unreliable() {
        val r1 = r(101, 1, 62724, 17.60, true)
        val r2 = r(102, 2, 63198, 27.36, false) // この区間は集計除外
        val r3 = r(103, 3, 63292, 15.03, true)
        val s = FuelEconomy.summarize(listOf(r1, r2, r3))
        // 集計対象は 63198→63292 の 94km / 15.03L のみ
        assertEquals(94.0 / 15.03, s.averageKmPerLiter!!, 0.001)
        assertEquals(63292 - 62724, s.totalDistanceKm)
    }

    @Test
    fun summarize_all_full_sums_all_intervals() {
        val r1 = r(101, 1, 62724, 17.60, true)
        val r2 = r(102, 2, 63198, 27.36, true)
        val r3 = r(103, 3, 63292, 15.03, true)
        val s = FuelEconomy.summarize(listOf(r1, r2, r3))
        // (474 + 94) / (27.36 + 15.03)
        assertEquals(568.0 / 42.39, s.averageKmPerLiter!!, 0.001)
    }
}
