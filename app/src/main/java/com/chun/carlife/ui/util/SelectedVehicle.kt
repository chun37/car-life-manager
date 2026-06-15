package com.chun.carlife.ui.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import com.chun.carlife.data.Vehicle

/** 画面間でユーザーが直前に選んでいた車両IDを共有する簡易ストア。 */
object SelectedVehicleStore {
    val state: MutableState<Long?> = mutableStateOf(null)

    private var lastSeenDefaultId: Long? = null

    /**
     * 現在の選択を `vehicles` / `defaultId` に追従させる。
     * デフォルト車両が前回観測時から切り替わったら新しい default を採用、
     * それ以外は現選択を維持 (無効なら default → 先頭で fallback)。
     */
    fun syncWithDefault(vehicles: List<Vehicle>, defaultId: Long?) {
        val defaultChanged = defaultId != lastSeenDefaultId
        lastSeenDefaultId = defaultId
        state.value = if (defaultChanged && defaultId != null && vehicles.any { it.id == defaultId }) {
            defaultId
        } else {
            resolveInitialVehicleId(state.value, vehicles, defaultId)
        }
    }
}

val LocalSelectedVehicleId = compositionLocalOf<MutableState<Long?>> { SelectedVehicleStore.state }
