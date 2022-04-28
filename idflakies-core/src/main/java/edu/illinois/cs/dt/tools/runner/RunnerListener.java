package edu.illinois.cs.dt.tools.runner;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RunnerListener extends RunListener {
    @Override
    public void testFinished(final Description description) throws Exception {
    }

    @Override
    public void testStarted(final Description description) throws Exception {
    }

    @Override
    public void testFailure(final Failure failure) throws Exception {
//        failure.getException().printStackTrace();
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
//        failure.getException().printStackTrace();
    }
}
