package com.chun.carlife.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.chun.carlife.CarLifeApp
import com.chun.carlife.data.AppDatabase
import com.chun.carlife.data.Vehicle

@Composable
fun rememberDatabase(): AppDatabase {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CarLifeApp
    return app.database
}

/**
 * アプリ全体で共有される車両リスト。値は `null` = まだロード中、
 * 一度emitされた後はタブを切り替えてもキャッシュされた値が返る。
 */
@Composable
fun rememberVehicles(): State<List<Vehicle>?> {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CarLifeApp
    return app.vehiclesFlow.collectAsState()
}

/** デフォルト車両ID。永続化されており、アプリ再起動後も保持される。 */
@Composable
fun rememberDefaultVehicleId(): State<Long?> {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CarLifeApp
    return app.defaultVehicleId.collectAsState()
}

@Composable
fun rememberSetDefaultVehicleId(): (Long?) -> Unit {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CarLifeApp
    return { app.setDefaultVehicleId(it) }
}

/** 既存の選択がなければ、デフォルト → 先頭 の順で fallback した ID を返す。 */
fun resolveInitialVehicleId(
    currentSelection: Long?,
    vehicles: List<Vehicle>,
    defaultId: Long?,
): Long? {
    if (currentSelection != null && vehicles.any { it.id == currentSelection }) return currentSelection
    val resolvedDefault = defaultId?.takeIf { id -> vehicles.any { it.id == id } }
    return resolvedDefault ?: vehicles.firstOrNull()?.id
}
