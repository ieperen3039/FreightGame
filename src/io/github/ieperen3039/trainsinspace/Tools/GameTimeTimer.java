package io.github.ieperen3039.trainsinspace.Tools;

import io.github.ieperen3039.ngn.Tools.TickTimeTimer;

import java.io.Serializable;

/**
 * Compared to RealTimeTimer, this timer is serializable, handles time jumps, and support time scaling
 */
public class GameTimeTimer implements TickTimeTimer, Serializable {
    private static final long MAX_JUMP_MILLIS = 500;
    private transient long trueLoopStartMillis;
    private double currentTime;
    private double previousTime;
    private double speedFactor = 1.0;

    public GameTimeTimer() {
        currentTime = 0;
        previousTime = 0;
        trueLoopStartMillis = System.currentTimeMillis();
    }

    @Override
    public void updateTime(double newTime) {
        previousTime = currentTime;
        currentTime = newTime;
        trueLoopStartMillis = System.currentTimeMillis();
    }

    public void setSpeedFactor(double speedFactor) {
        this.speedFactor = speedFactor;
    }

    /**
     * @return the time at the call of updateLooptime
     */
    @Override
    public double getTime() {
        return currentTime;
    }

    /**
     * @return The elapsed time in seconds between the previous two ticks.
     */
    @Override
    public double getDeltaTimeSeconds() {
        return currentTime - previousTime;
    }

    /**
     * @return game-seconds since the start of the current tick in seconds
     */
    @Override
    public double getSecondsSinceLastUpdate() {
        return getTrueSecondsSinceLastUpdate() * speedFactor;
    }

    /**
     * Adds a new tick at the current system time
     */
    @Override
    public void updateLoopTime() {
        updateTime(currentTime + getSecondsSinceLastUpdate());
    }

    /**
     * @return The number of real-time seconds since the start of this loop
     */
    private double getTrueSecondsSinceLastUpdate() {
        long now = System.currentTimeMillis();

        if (trueLoopStartMillis < now) {
            return 0;
        }

        if (now > trueLoopStartMillis + MAX_JUMP_MILLIS) {
            return MAX_JUMP_MILLIS;
        }

        return ((now - trueLoopStartMillis) / 1000.0);
    }
}
