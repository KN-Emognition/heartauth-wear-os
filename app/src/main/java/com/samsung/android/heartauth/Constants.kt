package com.samsung.android.heartauth


class Constants {
    companion object {
        const val MEASUREMENT_DURATION = 3000L
        const val ECG_SIGNAL_FREQ = 500
        const val ECG_LID_GRACE_PERIOD =  2_000L
        const val WATCH_NO_CONTACT_CODE = 5
        const val HAUTH_TAG = "HeartAuth-Ecg-Collector"
        const val ECG_LID_DEBOUNCE_TICKS = 2
        const val ECG_LID_STARTING_TICKS=200

    }
}