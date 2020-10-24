package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationTimeFilter;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RandomTimeDetector extends RandomDetector {

    public RandomTimeDetector(final String type, final Runner runner, final int rounds, final List<String> tests) {
        super(type, runner, rounds, tests);
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationTimeFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationTimeFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner)));
        }
    }

    @Override
    public DetectionRound makeDts(final TestRunResult intended, final TestRunResult revealed) {
        final List<DependentTest> result = DetectorUtil.flakyTimeTests(intended, revealed, countOnlyFirstFailure);

        return new DetectionRound(Collections.singletonList(revealed.id()),
                result,
                filter(result, absoluteRound.get()).collect(Collectors.toList()),
                stopwatch.elapsed(TimeUnit.NANOSECONDS) / 1E9);
    }
}
