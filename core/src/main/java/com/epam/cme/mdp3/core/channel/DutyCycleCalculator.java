package com.epam.cme.mdp3.core.channel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

public class DutyCycleCalculator implements DoubleSupplier {
    private final AtomicLong totalDutyTime;
    private final TimeUnit unit;


    private long lastTime = System.currentTimeMillis();
    private long lastTotalCaptureLatency = 0;


    public DutyCycleCalculator(AtomicLong totalDutyTime, TimeUnit unit) {
        this.totalDutyTime = totalDutyTime;
        this.unit = unit;
    }

    @Override
    public double getAsDouble() {
        // Called every time the stat is read (every 1 minute)

        long now = System.currentTimeMillis();
        long newTotalCaptureLatency = unit.toMillis(totalDutyTime.get()); // The total time spent "receiving" data since the process has started

        long elapsedTime = now - this.lastTime; // The time since this method was last called
        long captureTime = newTotalCaptureLatency - this.lastTotalCaptureLatency; // The time spent "receiving" data since the last call

        this.lastTime = now;
        this.lastTotalCaptureLatency = newTotalCaptureLatency;

        if (captureTime == 0) {
            return 0.0;

        } else {
            return (double) captureTime / elapsedTime;

        }
    }
}
