package edu.illinois.cs.dt.tools.detection.filters;

import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.runner.Runner;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;

public class VerifyFilter implements Filter {
    private static final boolean VERIFY_DTS = Configuration.config().getProperty("dt.verify", true);
    private final String detectorType;
    private final Runner runner;

    public VerifyFilter(final String detectorType, final Runner runner) {
        this.detectorType = detectorType;
        this.runner = runner;
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        if (VERIFY_DTS) {
            return dependentTest.verify(runner, DetectorPathManager.filterPath(detectorType, "verify", absoluteRound));
        } else {
            return true;
        }
    }
}
