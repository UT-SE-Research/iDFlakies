package edu.illinois.cs.dt.tools.detection.detectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;

public class RandomDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final TestShuffler testShuffler;

    public RandomDetector(final String type, final Runner runner, final int rounds, final List<String> tests) {
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

    @Override
    protected boolean triedAllOrders(int rounds) {
        long permutations = testShuffler.calculatePermutations();
        int numTried = testShuffler.ordersTried();
        if (numTried >= permutations) {
            return true;
        }
        return false;
    }

    @Override
    protected void printToFile(){
        testShuffler.saveOrders();
    }

}
