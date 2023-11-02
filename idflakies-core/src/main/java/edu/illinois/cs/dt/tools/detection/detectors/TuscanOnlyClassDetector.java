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
import java.util.ArrayList;
import java.util.List;

public class TuscanOnlyClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final TestShuffler testShuffler;

    public TuscanOnlyClassDetector(final Runner runner, final File baseDir, final int rounds, final String type,
            final List<String> tests) {
        super(runner, baseDir, rounds, type);
        int num = getClassesSize(tests);
        if (num == 3 || num == 5) {
            // We need one more round than the number of classes if n is 3 or 5.
            if (this.rounds > num) {
                this.rounds = num + 1;
            }
        } else {
            if (this.rounds > num) {
                this.rounds = num;
            }
        }
        this.tests = tests;
        this.testShuffler = new TestShuffler(type, rounds, tests, baseDir);
        this.origResult = DetectorUtil.originalResults(tests, runner);
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner, baseDir)));
        }
        addFilter(new UniqueFilter());
    }

    public static int getClassesSize(List<String> tests) {
        List<String> classes = new ArrayList<String>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classes.contains(className)) {
                classes.add(className);
            }
        }
        return classes.size();
    }

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(testShuffler.alphabeticalAndTuscanOrder(absoluteRound.get(), true)));
    }
}
