package edu.illinois.cs.dt.tools.detection.detectors;

import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.Runner;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;

import java.util.List;

public class OriginalDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    public OriginalDetector(final Runner runner, final int rounds, final List<String> tests, final TestRunResult origResult) {
        super(runner, rounds, "original");

        this.tests = tests;
        this.origResult = origResult;

        addFilter(new UniqueFilter());
    }

    public OriginalDetector(final Runner runner, final int rounds, final List<String> tests) {
        super(runner, rounds, "original");

        this.tests = tests;
        this.origResult = DetectorUtil.originalResults(tests, runner);

        addFilter(new UniqueFilter());
    }

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(tests));
    }
}
