package com.chun.carlife.ui.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/** 画面間でユーザーが直前に選んでいた車両IDを共有する簡易ストア。 */
object SelectedVehicleStore {
    val state: MutableState<Long?> = mutableStateOf(null)
}

val LocalSelectedVehicleId = compositionLocalOf<MutableState<Long?>> { SelectedVehicleStore.state }
