package edu.illinois.cs.dt.tools.runner.data;

import edu.illinois.cs.testrunner.data.results.Result;

import java.util.List;

public class TestRunWithTime extends TestRun {

    private final Double fullTime;

    public TestRunWithTime(final List<String> order, final Result result, final Double time, final Double fullTime, final String testRunId) {
        super(order, result, time, testRunId);
        this.fullTime = fullTime;
    }

    public Double fullTime() {
        return fullTime;
    }
}
