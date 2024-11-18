package edu.illinois.cs.testrunner.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;

public class TestInfo {
    public static <T> List<T> beforeInc(final List<T> ts, final T t) {
        final int i = ts.indexOf(t);

        if (i != -1) {
            return new ArrayList<>(ts.subList(0, Math.min(ts.size(), i + 1)));
        } else {
            return new ArrayList<>();
        }
    }

    // The key is all tests in the order, and the value is the result of the last test.
    // This is used to discover flaky dts during runs of this tool.
    private final Map<List<String>, Result> knownRuns = new HashMap<>();

    private final List<Double> times = new ArrayList<>();

    private final String testName;
    private boolean isFlaky = false;

    public TestInfo(final List<String> order, final String testName, final TestResult result)
            throws FlakyTestException {
        this.testName = testName;
        updateWith(order, result);
    }

    public void updateWith(final List<String> order, final TestResult result) throws FlakyTestException {
        updateTime(result);
        updateFlakiness(order, result);
    }

    private void updateTime(final TestResult result) {
        times.add(result.time());
    }

    private void updateFlakiness(List<String> order, TestResult result) throws FlakyTestException {
        if (!isFlaky) {
            final Result testResult = result.result();

            final List<String> testsBefore = beforeInc(order, testName);

            if (knownRuns.containsKey(testsBefore) && !knownRuns.get(testsBefore).equals(testResult)) {
                this.isFlaky = true;

//                throw new FlakyTestException(testName, knownRuns.get(testsBefore), testResult, testsBefore);
            } else {
                knownRuns.put(testsBefore, testResult);
            }
        }
    }

    public double averageTime() {
        return times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public boolean isFlaky() {
        return isFlaky;
    }
}
