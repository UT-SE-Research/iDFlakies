package edu.illinois.cs.dt.tools.detection.detectors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

public class TuscanIntraClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final HashMap<String, List<String>> classToMethods;
    private final TestShuffler testShuffler;

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
    
    public TuscanIntraClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
        super(runner, baseDir, rounds, type);
        // int n = getClassesSize(tests);
        // if (n == 3 || n == 5) {
        //     if (this.rounds > n) {
        //             this.rounds = n + 1;
        //     }
        // } else {
        //     if (this.rounds > n) {
        //         this.rounds = n;
        //     }
        // }
        classToMethods = new HashMap<>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }
            classToMethods.get(className).add(test);
        }
        int classSize = classToMethods.keySet().size();
        int maxMethodSize = 0;
        for (String className : classToMethods.keySet()) {
            int nn = classToMethods.get(className).size();
            if(maxMethodSize < nn) {
                maxMethodSize = nn;
            }
        }
        if (classSize > maxMethodSize) {
            this.rounds = classSize;
        } else {
            this.rounds = maxMethodSize;
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

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(testShuffler.tuscanIntraClassOrder(absoluteRound.get())));
    }
}
