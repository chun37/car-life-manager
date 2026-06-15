package com.chun.carlife

import android.app.Application
import com.chun.carlife.data.AppDatabase

class CarLifeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
}
