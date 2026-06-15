package com.chun.carlife.domain

import com.chun.carlife.data.Vehicle

enum class EnergyKind {
    FUEL,
    ELECTRIC;

    companion object {
        fun from(raw: String?): EnergyKind =
            entries.firstOrNull { it.name == raw } ?: FUEL
    }
}

val Vehicle.energy: EnergyKind get() = EnergyKind.from(energyKind)
