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

public class TuscanInterClassDetector extends ExecutingDetector {
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

    public static int findNumberOfRounds (HashMap<String, List<String>> classToMethods) {
        int classSize = classToMethods.keySet().size();
        if (classSize == 3 || classSize == 5) {
            classSize++;
        }
        int totalMethodSize = 1;
        for (String className : classToMethods.keySet()) {
            int methodSize = classToMethods.get(className).size();
            if (methodSize == 3 || methodSize == 5) {
                totalMethodSize *= (methodSize + 1);
            } else {
                totalMethodSize *= methodSize; 
            }
        }
        int tempRounds = classSize * totalMethodSize;
        return tempRounds;
    }
    
    public TuscanInterClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
        super(runner, baseDir, rounds, type);
        classToMethods = new HashMap<>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }
            classToMethods.get(className).add(test);
        }
        // int classSize = classToMethods.keySet().size();
        // if (classSize == 3 || classSize == 5) {
        //     classSize++;
        // }
        // int totalMethodSize = 1;
        // for (String className : classToMethods.keySet()) {
        //     int methodSize = classToMethods.get(className).size();
        //     if (methodSize == 3 || methodSize == 5) {
        //         totalMethodSize *= (methodSize + 1);
        //     } else {
        //         totalMethodSize *= methodSize; 
        //     }
        // }
        // int tempRounds = classSize * totalMethodSize;
        int tempRounds = findNumberOfRounds(classToMethods);
        if (rounds > tempRounds) {
            this.rounds = tempRounds;
        } else {
            this.rounds = rounds;
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
        return makeDts(origResult, runList(testShuffler.tuscanInterClass(absoluteRound.get())));
    }
}
