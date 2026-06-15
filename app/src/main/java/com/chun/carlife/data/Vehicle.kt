package com.chun.carlife.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val maker: String = "",
    val model: String = "",
    val plateNumber: String = "",
    val initialOdometer: Int = 0,
    val fuelTankCapacityLiters: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
