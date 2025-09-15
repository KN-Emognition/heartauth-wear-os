package com.samsung.android.ecgmonitor.base.model


class EcgAverager {
    private var lastAvg = 0.0

    fun update(newAvg: Double): Double {
        lastAvg = newAvg
        return lastAvg
    }

    fun get(): Double {
        return lastAvg
    }
}
