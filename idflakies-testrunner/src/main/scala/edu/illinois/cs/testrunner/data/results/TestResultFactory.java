package edu.illinois.cs.testrunner.data.results;

import junit.framework.AssertionFailedError;
import org.junit.ComparisonFailure;

public class TestResultFactory {
    public static boolean isAssertFailure(final Throwable exception) {
        return exception.getClass().equals(AssertionFailedError.class) ||
                exception.getClass().equals(ComparisonFailure.class);
    }

    public static TestResult passing(final double time, final String testName) {
        return new TestResult(testName, Result.PASS, time, new String[0]);
    }

    public static TestResult missing(final String testName) {
        return new TestResult(testName, Result.SKIPPED, -1, new String[0]);
    }

    public static TestResult ignored(final String fullMethodName) {
        return new TestResult(fullMethodName, Result.SKIPPED, -1, new String[0]);
    }

    public static TestResult failOrError(final Throwable throwable, final double time, final String testName) {
        //check whether it is a failure or an error
        final Result result;
        if(isAssertFailure(throwable)) {
            result = Result.FAILURE;
        } else {
            result = Result.ERROR;
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String[] stackTraceStrings = new String[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            stackTraceStrings[i] = stackTrace[i].toString();
        }
        return new TestResult(testName, result, time, stackTraceStrings);
    }
}
