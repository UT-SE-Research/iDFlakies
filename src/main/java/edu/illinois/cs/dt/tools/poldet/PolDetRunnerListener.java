package edu.illinois.cs.dt.tools.poldet;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class PolDetRunnerListener extends RunListener {

    @Override
    public void testStarted(final Description description) throws Exception {
        StateCapture.captureBefore(description.getClassName() + "." + description.getMethodName());
    }

    @Override
    public void testFinished(final Description description) throws Exception {
        StateCapture.captureAfter(description.getClassName() + "." + description.getMethodName());
    }

}
