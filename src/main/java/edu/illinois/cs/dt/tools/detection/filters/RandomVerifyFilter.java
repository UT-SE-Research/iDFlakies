package edu.illinois.cs.dt.tools.detection.filters;

import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.detection.filters.Filter;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.testrunner.runner.Runner;

import java.util.Random;

public class RandomVerifyFilter implements Filter {
    private final double percentage;
    private final String detectorType;
    private final Runner runner;

    public RandomVerifyFilter(final double percentage, final String detectorType, final Runner runner) {
        this.percentage = percentage;
        this.detectorType = detectorType;
        this.runner = runner;
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        // Only confirm in percentage% of the runs
        if (new Random().nextDouble() < percentage) {
            return dependentTest.verify(runner, DetectorPathManager.filterPath(detectorType, "confirmation-sampling", absoluteRound));
        }

        return true;
    }
}
