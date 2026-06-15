package com.chun.carlife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Vehicle::class, Refuel::class, Maintenance::class, ScheduleOverride::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun refuelDao(): RefuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun scheduleOverrideDao(): ScheduleOverrideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "car-life.db",
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
