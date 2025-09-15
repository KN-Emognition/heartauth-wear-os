package com.samsung.android.ecgmonitor;


import android.content.Context;

import com.samsung.android.ecgmonitor.R;

import java.util.Locale;

public final class UiTextFormatter {
    public static String measuringUpdate(Context ctx, long secondsLeft, double avgMv) {
        return ctx.getString(R.string.MeasurementUpdate, secondsLeft, String.format(Locale.ENGLISH, "%.2f", avgMv));
    }
    public static String success(Context ctx, double avgMv) {
        return ctx.getString(R.string.MeasurementSuccessful, String.format(Locale.ENGLISH, "%.2f", avgMv));
    }
}

