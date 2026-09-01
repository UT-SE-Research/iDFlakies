package edu.illinois.cs.dt.tools.detection.detectors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

public class TuscanIntraClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final HashMap<String, List<String>> classToMethods;
    private final TestShuffler testShuffler;
    private List<List<String>> orders;
    private int num_of_order = Integer.MAX_VALUE;
    
    public static int findMaxMethodSize(HashMap<String, List<String>> classToMethods) {
        int maxMethodSize = 0;
        for (String className : classToMethods.keySet()) {
            int nn = classToMethods.get(className).size();
            if(maxMethodSize < nn) {
                maxMethodSize = nn;
            }
        }
        return maxMethodSize;
    }
    
    public TuscanIntraClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
        super(runner, baseDir, rounds, type);
        orders = new ArrayList<>();
        classToMethods = new HashMap<>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }
            classToMethods.get(className).add(test);
        }
        int classSize = classToMethods.keySet().size();
        if (classSize == 3 || classSize == 5) {
            classSize++;
        }
        int maxMethodSize = findMaxMethodSize(classToMethods);
        if (maxMethodSize == 3 || maxMethodSize == 5) {
            maxMethodSize++;
        }
        if (classSize > maxMethodSize) {
            if (num_of_order > classSize) {
                num_of_order = classSize;
            }
        } else {
            if (num_of_order > maxMethodSize) {
                num_of_order = maxMethodSize;
            }
        }
        this.tests = tests;
	    String s = Integer.toString(num_of_order);
        Logger.getGlobal().log(Level.INFO, "INITIAL CALCULATED NUM OF ORDERS: " + num_of_order);
        this.testShuffler = new TestShuffler(type, num_of_order, tests, baseDir);
        this.origResult = DetectorUtil.originalResults(tests, runner);
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner, baseDir)));
        }
        addFilter(new UniqueFilter());
	    Set<List<String>> ordersSet = new HashSet<>();
        int num = 0;
        for (int i = 0; i < num_of_order; i ++) {
            List<String> order = testShuffler.tuscanIntraClassOrder(i);
            if (!ordersSet.contains(order)) {
                ExecutingDetector.writeOrder(order, PathManager.ordersPath(), num, tests);
		        num ++;
		        orders.add(order);
            } else {
                ordersSet.add(order);
            }
	    }
	    s = Integer.toString(num);
	    writeTo(PathManager.numOfOrdersPath(), s);
        Logger.getGlobal().log(Level.INFO, "UPDATED CALCULATED NUM OF ORDERS: " + num);
        num_of_order = num;
        if (num < this.rounds) {
            this.rounds = num;
        }
    }

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(orders.get(absoluteRound.get())));
    }
}
