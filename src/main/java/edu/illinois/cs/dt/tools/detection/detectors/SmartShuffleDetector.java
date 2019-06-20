package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.SmartShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.util.List;

public class SmartShuffleDetector extends ExecutingDetector {
    private final List<String> originalOrder;
    private final TestRunResult originalResults;

    private final SmartShuffler shuffler;

    public SmartShuffleDetector(final InstrumentingSmartRunner runner,
                                final int rounds, final List<String> tests,
                                final String type) {
        super(runner, rounds, type);

        this.originalOrder = tests;
        this.shuffler = new SmartShuffler(tests);
        this.originalResults = DetectorUtil.originalResults(originalOrder, runner);

        addFilter(new ConfirmationFilter(type, tests, runner));
        addFilter(new UniqueFilter());
    }

    @Override
    public DetectionRound results() throws Exception {
        final List<String> order = shuffler.nextOrder();

        return makeDts(originalResults, runList(order));
    }
}
