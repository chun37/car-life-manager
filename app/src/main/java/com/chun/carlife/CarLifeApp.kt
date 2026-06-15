package com.chun.carlife

import android.app.Application
import android.content.SharedPreferences
import com.chun.carlife.data.AppDatabase
import com.chun.carlife.data.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

private const val PREFS_NAME = "car-life-prefs"
private const val KEY_DEFAULT_VEHICLE_ID = "default_vehicle_id"

class CarLifeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    /** null = まだ未ロード。一度ロードされた後はタブを切り替えてもキャッシュ値を返す。 */
    val vehiclesFlow: StateFlow<List<Vehicle>?> by lazy {
        database.vehicleDao().observeAll()
            .stateIn(appScope, SharingStarted.Eagerly, null)
    }

    private val _defaultVehicleId = MutableStateFlow<Long?>(null)
    val defaultVehicleId: StateFlow<Long?> = _defaultVehicleId.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        _defaultVehicleId.value = prefs.getLong(KEY_DEFAULT_VEHICLE_ID, -1L).takeIf { it != -1L }
    }

    fun setDefaultVehicleId(id: Long?) {
        _defaultVehicleId.value = id
        prefs.edit().apply {
            if (id == null) remove(KEY_DEFAULT_VEHICLE_ID)
            else putLong(KEY_DEFAULT_VEHICLE_ID, id)
        }.apply()
    }
}
