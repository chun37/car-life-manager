package com.chun.carlife.domain

import com.chun.carlife.data.Maintenance
import java.util.Calendar

data class ScheduleItem(
    val category: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
)

/** 一般的なメンテナンス周期のデフォルト値。個別チューニングは後でやる前提。 */
val DEFAULT_SCHEDULE: List<ScheduleItem> = listOf(
    ScheduleItem("オイル交換", intervalKm = 5_000, intervalMonths = 6),
    ScheduleItem("オイルフィルター", intervalKm = 10_000, intervalMonths = 12),
    ScheduleItem("タイヤローテーション", intervalKm = 10_000, intervalMonths = 6),
    ScheduleItem("タイヤ交換", intervalKm = 50_000, intervalMonths = 60),
    ScheduleItem("ブレーキ", intervalKm = 40_000, intervalMonths = 24),
    ScheduleItem("バッテリー", intervalMonths = 36),
    ScheduleItem("車検", intervalMonths = 24),
    ScheduleItem("12ヶ月点検", intervalMonths = 12),
)

data class ScheduleStatus(
    val item: ScheduleItem,
    val lastDate: Long?,
    val lastOdometer: Int?,
    /** 残り走行距離。null = 履歴なし or 距離基準なし。負値 = 超過。 */
    val kmLeft: Int?,
    /** 残り日数。null = 履歴なし or 月基準なし。負値 = 超過。 */
    val daysLeft: Int?,
) {
    val hasHistory: Boolean get() = lastDate != null

    val isOverdue: Boolean
        get() = (kmLeft != null && kmLeft <= 0) || (daysLeft != null && daysLeft <= 0)

    val isSoon: Boolean
        get() = !isOverdue && (
            (kmLeft != null && kmLeft <= 500) ||
                (daysLeft != null && daysLeft <= 30)
        )
}

data class IntervalOverride(val intervalKm: Int?, val intervalMonths: Int?)

object MaintenanceSchedule {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun computeStatuses(
        maintenances: List<Maintenance>,
        currentOdometer: Int,
        now: Long = System.currentTimeMillis(),
        schedule: List<ScheduleItem> = DEFAULT_SCHEDULE,
        overrides: Map<String, IntervalOverride> = emptyMap(),
    ): List<ScheduleStatus> = schedule.map { base ->
        val item = overrides[base.category]?.let {
            base.copy(intervalKm = it.intervalKm, intervalMonths = it.intervalMonths)
        } ?: base
        val last = maintenances.filter { it.category == item.category }.maxByOrNull { it.date }
        val kmLeft = if (item.intervalKm != null && last != null) {
            (last.odometer + item.intervalKm) - currentOdometer
        } else null
        val daysLeft = if (item.intervalMonths != null && last != null) {
            val due = Calendar.getInstance().apply {
                timeInMillis = last.date
                add(Calendar.MONTH, item.intervalMonths)
            }.timeInMillis
            ((due - now) / MILLIS_PER_DAY).toInt()
        } else null
        ScheduleStatus(item, last?.date, last?.odometer, kmLeft, daysLeft)
    }
}
