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
