package com.chun.carlife.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.chun.carlife.CarLifeApp
import com.chun.carlife.data.AppDatabase

@Composable
fun rememberDatabase(): AppDatabase {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CarLifeApp
    return app.database
}
