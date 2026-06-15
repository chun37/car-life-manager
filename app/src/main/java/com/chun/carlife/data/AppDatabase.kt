package com.chun.carlife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Vehicle::class, Refuel::class, Maintenance::class, ScheduleOverride::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun refuelDao(): RefuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun scheduleOverrideDao(): ScheduleOverrideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `schedule_overrides` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`vehicleId` INTEGER NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`intervalKm` INTEGER, " +
                        "`intervalMonths` INTEGER, " +
                        "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_schedule_overrides_vehicleId_category` " +
                        "ON `schedule_overrides` (`vehicleId`, `category`)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE vehicles ADD COLUMN energyKind TEXT NOT NULL DEFAULT 'FUEL'",
                )
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "car-life.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}
