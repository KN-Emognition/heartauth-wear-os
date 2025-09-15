package com.samsung.android.ecgmonitor

import android.content.Context
import java.util.Locale


object UiTextFormatter {
    @JvmStatic
    fun measuringUpdate(ctx: Context, secondsLeft: Long, avgMv: Double): String {
        return ctx.getString(
            R.string.MeasurementUpdate,
            secondsLeft,
            String.format(Locale.ENGLISH, "%.2f", avgMv)
        )
    }

    @JvmStatic
    fun success(ctx: Context, avgMv: Double): String {
        return ctx.getString(
            R.string.MeasurementSuccessful,
            String.format(Locale.ENGLISH, "%.2f", avgMv)
        )
    }
}

