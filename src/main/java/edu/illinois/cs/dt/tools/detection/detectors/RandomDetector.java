package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import edu.illinois.cs.testrunner.runner.Runner;

import java.util.ArrayList;
import java.util.List;

public class RandomDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final TestShuffler testShuffler;

    public RandomDetector(final String type, final Runner runner, final int rounds, final List<String> tests) {
        super(runner, rounds, type);

        this.tests = new ArrayList<>(tests);

        this.origResult = DetectorUtil.originalResults(tests, runner);
        // If not all passing, remove the tests that are not passing
        if (!DetectorUtil.allPass(this.origResult)) {
            for (String test : this.origResult.testOrder()) {
                Result res = this.origResult.results().get(test).result();
                if (!res.equals(Result.PASS) && !res.equals(Result.SKIPPED)) {
                    TestPluginPlugin.info("Removing " + test + ", " + res + ", " + this.origResult.id());
                    this.tests.remove(test);
                }
            }
        }
        this.testShuffler = new TestShuffler(type, rounds, tests);

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
