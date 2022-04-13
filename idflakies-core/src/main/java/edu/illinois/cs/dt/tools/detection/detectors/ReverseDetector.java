package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.io.File;
import java.util.List;

public class ReverseDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;
    private final TestShuffler testShuffler;

    public ReverseDetector(final Runner runner, final File baseDir, final int rounds, final String name, final List<String> tests) {
        // Always 1 round, because there's only one way to reverse the tests
        super(runner, baseDir, 1, name);
        this.tests = tests;
        this.origResult = DetectorUtil.originalResults(tests, runner);

        testShuffler = new TestShuffler(name, rounds, tests, baseDir);

        // Filters to be applied in order
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner, baseDir)));
        }
        addFilter(new UniqueFilter());
    }

    @Override
    public DetectionRound results() throws Exception {
        final List<String> reversed = testShuffler.shuffledOrder(absoluteRound.get());
        return makeDts(origResult, runList(reversed));
    }
}
