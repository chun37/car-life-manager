package com.chun.carlife.domain

import com.chun.carlife.data.Maintenance
import java.util.Calendar

data class ScheduleItem(
    val category: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
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

object MaintenanceSchedule {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun computeStatuses(
        maintenances: List<Maintenance>,
        currentOdometer: Int,
        schedule: List<ScheduleItem>,
        now: Long = System.currentTimeMillis(),
    ): List<ScheduleStatus> = schedule.map { item ->
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
