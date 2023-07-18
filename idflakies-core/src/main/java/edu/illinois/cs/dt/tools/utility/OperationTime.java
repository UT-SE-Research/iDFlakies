package edu.illinois.cs.dt.tools.utility;

import com.reedoei.eunomia.functional.ThrowingBiFunction;

import java.util.concurrent.Callable;

public class OperationTime {
    public static <T,U> U runOperation(final Callable<T> callable,
                                       final ThrowingBiFunction<T, OperationTime, U> constructor)
            throws Exception {
        final long startTime = System.currentTimeMillis();
        final T t = callable.call();
        final long endTime = System.currentTimeMillis();

        return constructor.apply(t, new OperationTime(startTime, endTime));
    }

    public static OperationTime instantaneous() {
        return new OperationTime(System.currentTimeMillis(), System.currentTimeMillis());
    }

    private final long startTime;
    private final long endTime;
    private final double elapsedSeconds;

    public OperationTime(final long startTime, final long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.elapsedSeconds = endTime / 1000.0 - startTime / 1000.0;
    }

    private OperationTime(final long startTime, final double elapsedSeconds) {
        this.startTime = startTime;
        this.elapsedSeconds = elapsedSeconds;
        this.endTime = (long) ((elapsedSeconds + (startTime / 1000.0)) * 1000.0);
    }

    public long startTime() {
        return startTime;
    }

    public long endTime() {
        return endTime;
    }

    public double elapsedSeconds() {
        return elapsedSeconds;
    }

    public OperationTime addTime(OperationTime otherTime) {
        if (otherTime == null) {
            return this;
        }
        long earliestStart = (startTime < otherTime.startTime) ? startTime : otherTime.startTime;
        double elapsedSeconds = elapsedSeconds() + otherTime.elapsedSeconds;
        return new OperationTime(earliestStart, elapsedSeconds);
    }

    public OperationTime mergeTime(OperationTime otherTime) {
        if (otherTime == null) {
            return this;
        }
        long earliestStart = (startTime < otherTime.startTime) ? startTime : otherTime.startTime;
        long latestEnd = (endTime < otherTime.endTime) ? otherTime.endTime: endTime;
        return new OperationTime(earliestStart, latestEnd);
    }
}
