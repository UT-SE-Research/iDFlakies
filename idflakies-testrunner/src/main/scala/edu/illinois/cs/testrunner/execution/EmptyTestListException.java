package edu.illinois.cs.testrunner.execution;

import java.util.List;

public class EmptyTestListException extends RuntimeException {
    public EmptyTestListException(final List<JUnitTest> tests) {
        super("No tests remaining for test order: " + tests);
    }
}
