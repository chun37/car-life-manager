package com.chun.carlife.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?

    @Query("SELECT * FROM vehicles")
    suspend fun getAll(): List<Vehicle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)
}

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<Refuel>>

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY date ASC, id ASC")
    suspend fun listByVehicleAsc(vehicleId: Long): List<Refuel>

    @Query("SELECT * FROM refuels ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<Refuel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(refuel: Refuel): Long

    @Delete
    suspend fun delete(refuel: Refuel)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenances WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(maintenance: Maintenance): Long

    @Delete
    suspend fun delete(maintenance: Maintenance)
}
