package edu.illinois.cs.dt.tools.detection.filters;

import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.runner.Runner;

import java.io.File;

public class VerifyFilter implements Filter {
    private static final boolean VERIFY_DTS = Configuration.config().getProperty("dt.verify", true);
    private final String detectorType;
    private final Runner runner;
    private final File baseDir;

    public VerifyFilter(final String detectorType, final Runner runner, final File baseDir) {
        this.detectorType = detectorType;
        this.runner = runner;
        this.baseDir = baseDir;
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        if (VERIFY_DTS) {
            return dependentTest.verify(runner, DetectorPathManager.filterPath(baseDir, detectorType, "verify", absoluteRound));
        } else {
            return true;
        }
    }
}
