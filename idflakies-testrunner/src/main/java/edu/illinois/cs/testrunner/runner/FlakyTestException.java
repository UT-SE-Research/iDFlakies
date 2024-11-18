package edu.illinois.cs.testrunner.runner;

import java.util.List;

import edu.illinois.cs.testrunner.data.results.Result;

public class FlakyTestException extends RuntimeException {
    private final String testName;
    private final Result result;
    private final Result testResult;
    private final List<String> testsBefore;

    public FlakyTestException(final String testName,
                              final Result result,
                              final Result testResult,
                              final List<String> testsBefore) {
        super(String.format("Flaky test '%s' found. Result was both %s and %s when run in order %s", testName, result, testResult, testsBefore));

        this.testName = testName;
        this.result = result;
        this.testResult = testResult;
        this.testsBefore = testsBefore;
    }

    public String testName() {
        return testName;
    }

    public Result result() {
        return result;
    }

    public Result testResult() {
        return testResult;
    }

    public List<String> testsBefore() {
        return testsBefore;
    }

}
