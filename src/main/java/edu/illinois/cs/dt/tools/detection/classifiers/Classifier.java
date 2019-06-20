package edu.illinois.cs.dt.tools.detection.classifiers;

import edu.illinois.cs.testrunner.data.results.TestRunResult;

public interface Classifier extends AutoCloseable {
    void update(final TestRunResult testRunResult);
}
