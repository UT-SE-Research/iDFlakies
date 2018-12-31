package edu.illinois.cs.dt.tools.detection.classifiers;

import com.reedoei.testrunner.data.results.TestRunResult;

public interface Classifier extends AutoCloseable {
    void update(final TestRunResult testRunResult);
}
