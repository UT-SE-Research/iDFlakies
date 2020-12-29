package edu.illinois.cs.dt.tools.detection.filters;

import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.testrunner.runner.SmartRunner;

public class FlakyFilter implements Filter {
    private final SmartRunner runner;

    public FlakyFilter(final SmartRunner runner) {
        this.runner = runner;
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        return !runner.info().isFlaky(dependentTest.name());
    }
}
