package edu.illinois.cs.dt.tools.utility;


/**
 * Created by winglam on 2/5/19.
 */
public class TimeManager {

    public TimeManager manageTime(OperationTime time) {
        this.addTime = addTime().addTime(time);
        this.totalTime = totalTime().mergeTime(time);
        return this;
    }

    public TimeManager mergeTimeManager(TimeManager tm) {
        this.addTime = addTime().addTime(tm.addTime());
        this.totalTime = totalTime().mergeTime(tm.totalTime());
        return this;
    }

    private OperationTime addTime;
    private OperationTime totalTime;

    public TimeManager(final OperationTime addTime, final OperationTime totalTime) {
        this.addTime = addTime;
        this.totalTime = totalTime;
    }

    public TimeManager(final TimeManager other) {
        this.addTime = other.addTime();
        this.totalTime = other.totalTime();
    }

    public OperationTime addTime() {
        return addTime;
    }

    public OperationTime totalTime() {
        return totalTime;
    }
}
