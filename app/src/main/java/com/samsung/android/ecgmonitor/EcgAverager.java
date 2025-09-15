package com.samsung.android.ecgmonitor;


public final class EcgAverager {
    private double lastAvg = 0;

    public double update(double newAvg) {
        lastAvg = newAvg;
        return lastAvg;
    }

    public double get() {
        return lastAvg;
    }
}
