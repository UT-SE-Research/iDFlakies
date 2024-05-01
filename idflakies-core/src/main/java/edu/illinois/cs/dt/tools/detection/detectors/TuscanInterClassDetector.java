package edu.illinois.cs.dt.tools.detection.detectors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.dt.tools.utility.Tuscan;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

public class TuscanInterClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final HashMap<String, List<String>> classToMethods;
    private final TestShuffler testShuffler;
    private List<List<String>> orders;
    private int num_of_order = Integer.MAX_VALUE;

    public static int findNumberOfRounds(HashMap<String, List<String>> classToMethods) {
        int classSize = classToMethods.keySet().size();
        List<String> classes = new ArrayList<>(classToMethods.keySet());
        Collections.sort(classes);
        int[][] classPermutations = Tuscan.generateTuscanPermutations(classSize);
        HashMap<String, Integer> classToSize = new HashMap<>();
        for (String className : classToMethods.keySet()) {
            classToSize.put(className, classToMethods.get(className).size());
        }
        int tempRounds = 0;
        for (int i = 0; i < classPermutations.length; i++) {
            int methodSize = 0;
            for (int j = 0; j < classPermutations[i].length - 2; j++) {
                String current = classes.get(classPermutations[i][j]);
                String next = classes.get(classPermutations[i][j + 1]);
                int size1 = classToMethods.get(current).size();
                if (size1 == 3 || size1 == 5) {size1 += 1;}
                int size2 = classToMethods.get(next).size();
                if (size2 == 3 || size2 == 5) {size2 += 1;}
                methodSize += (size1 * size2);
            }
            tempRounds += methodSize;
        }
        return tempRounds;
    }
    
    public TuscanInterClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
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
        int tempRounds = findNumberOfRounds(classToMethods);
        if (num_of_order > tempRounds) {
            num_of_order = tempRounds;
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
            List<String> order = testShuffler.tuscanInterClass(i);
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
