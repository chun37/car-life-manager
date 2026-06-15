package com.chun.carlife

import android.app.Application
import com.chun.carlife.data.AppDatabase
import com.chun.carlife.data.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CarLifeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** null = まだ未ロード。一度ロードされた後はタブを切り替えてもキャッシュ値を返す。 */
    val vehiclesFlow: StateFlow<List<Vehicle>?> by lazy {
        database.vehicleDao().observeAll()
            .stateIn(appScope, SharingStarted.Eagerly, null)
    }
}
