package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.util.List;

public class IncrementalDetector extends ExecutingDetector{
    private final List<String> tests;
    private TestRunResult origResult;

    private final TestShuffler testShuffler;

    public IncrementalDetector(final String type, final Runner runner, final int rounds, final List<String> tests) {
        super(runner, rounds, type);

        this.tests = tests;

        this.testShuffler = new TestShuffler(type, rounds, tests);
        this.origResult = DetectorUtil.originalResults(tests, runner);

        // Filters to be applied in order
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner)));
        }

        addFilter(new UniqueFilter());
    }

    @Override
    public DetectionRound results() throws Exception {
        final List<String> fullTestOrder = testShuffler.shuffledOrder(absoluteRound.get());

        return makeDts(origResult, runList(fullTestOrder));
    }
}
