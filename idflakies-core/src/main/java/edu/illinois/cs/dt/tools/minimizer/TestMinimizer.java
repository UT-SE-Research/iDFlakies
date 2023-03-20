package edu.illinois.cs.dt.tools.minimizer;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.data.caching.FileCache;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.RuntimeThrower;
import com.reedoei.eunomia.util.Util;

import edu.illinois.cs.dt.tools.minimizer.cleaner.CleanerData;
import edu.illinois.cs.dt.tools.minimizer.cleaner.CleanerFinder;
import edu.illinois.cs.dt.tools.minimizer.cleaner.CleanerGroup;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.SmartRunner;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestMinimizer extends FileCache<MinimizeTestsResult> {

    protected final List<String> testOrder;
    protected final String dependentTest;
    protected final Result expected;
    protected final Result isolationResult;
    protected final SmartRunner runner;
    protected final List<String> fullTestOrder;
    // By setting ONE_BY_ONE_POLLUTERS, will find all the polluters
    private final boolean ONE_BY_ONE_POLLUTERS = Configuration.config().getProperty("dt.minimizer.polluters.one_by_one", false);
    // Fully quailified polluter test names seperated by ';' (overwrite the order to look for polluters)
    private final String CUSTOM_POLLUTERS = Configuration.config().getProperty("dt.minimizer.polluters.custom", "");
    // FIND_ALL only for cleaners, not for polluters
    private static final boolean FIND_ALL = Configuration.config().getProperty("dt.find_all", true);

    protected final Path path;

    protected TestRunResult expectedRun;

    private void debug(final String str) {
        Logger.getGlobal().log(Level.FINE, str);
    }

    private void info(final String str) {
        Logger.getGlobal().log(Level.INFO, str);
    }

    public TestMinimizer(final List<String> testOrder, final SmartRunner runner, final String dependentTest) {
        // Only take the tests that come before the dependent test
        this.fullTestOrder = testOrder;
        this.testOrder = testOrder.contains(dependentTest) ? ListUtil.before(testOrder, dependentTest) : testOrder;
        this.dependentTest = dependentTest;

        this.runner = runner;

        // Run in given order to determine what the result should be.
        debug("Getting expected result for: " + dependentTest);
        this.expectedRun = runResult(testOrder);
        this.expected = expectedRun.results().get(dependentTest).result();
        this.isolationResult = result(Collections.singletonList(dependentTest));
        debug("Expected: " + expected);

        this.path = PathManager.minimizedPath(dependentTest, MD5.hashOrder(expectedRun.testOrder()), expected);
    }

    public Result expected() {
        return expected;
    }

    private TestRunResult runResult(final List<String> order) {
        final List<String> actualOrder = new ArrayList<>(order);

        if (!actualOrder.contains(dependentTest)) {
            actualOrder.add(dependentTest);
        }

        return runner.runList(actualOrder).get();
    }

    private Result result(final List<String> order) {
        try {
            return runResult(order).results().get(dependentTest).result();
        } catch (java.lang.IllegalThreadStateException e) {
             // indicates timeout
            return Result.SKIPPED;
        }
    }

    public MinimizeTestsResult run() throws Exception {
        final long startTime = System.currentTimeMillis();
        return OperationTime.runOperation(() -> {
            info("Running minimizer for: " + dependentTest + " (expected result in this order: " + expected + ")");

            // Keep going as long as there are tests besides dependent test to run
            List<PolluterData> polluters = new ArrayList<>();
            int index = 0;

            if (ONE_BY_ONE_POLLUTERS) {
                info("Getting all polluters (dt.minimizer.polluters.one_by_one is set to true)");
                List<String> testOrderToGetPolluters = fullTestOrder;
                if (CUSTOM_POLLUTERS != "") {
                    testOrderToGetPolluters = new ArrayList<String>(Arrays.asList(CUSTOM_POLLUTERS.split(";")));
                }
                for (List<String> order : getSingleTests(testOrderToGetPolluters, dependentTest)) {
                    index = getPolluters(order, startTime, polluters, index);
                }
            } else {
                List<String> order = new ArrayList<>(testOrder);
                if (CUSTOM_POLLUTERS != "") {
                    order = new ArrayList<String>(Arrays.asList(CUSTOM_POLLUTERS.split(";")));
                }
                getPolluters(order, startTime, polluters, index);
            }

            return polluters;
        }, (polluters, time) -> {
            final MinimizeTestsResult minimizedResult =
                    new MinimizeTestsResult(time, expectedRun, expected, dependentTest, polluters, FlakyClass.OD);

            // If the verifying does not work, then mark this test as NOD
            boolean verifyStatus = minimizedResult.verify(runner);
            if (verifyStatus) {
                return minimizedResult;
            } else {
                return new MinimizeTestsResult(time, expectedRun, expected, dependentTest, polluters, FlakyClass.NOD);
            }
        });
    }

    private int getPolluters(List<String> order, long startTime, List<PolluterData> polluters, int index) throws Exception {
        // order can be the prefix + dependentTest or just the prefix. All current uses of this method are using it as just prefix
        while (!order.isEmpty()) {
            // First need to check if remaining tests in order still lead to expected value
            if (result(order) != expected) {
                info("Remaining tests no longer match expected: " + order);
                break;
            }

            final OperationTime[] operationTime = new OperationTime[1];
            final List<String> deps = OperationTime.runOperation(() -> {
                return run(new ArrayList<>(order));
            }, (foundDeps, time) -> {
                operationTime[0] = time;
                return foundDeps;
            });

            if (deps.isEmpty()) {
                info("Did not find any deps");
                break;
            }

            info("Ran minimizer, dependencies: " + deps);
            double elapsedSeconds = System.currentTimeMillis() / 1000.0 - startTime / 1000.0;
            if (index == 0) {
                info("FIRST POLLUTER: Found first polluter " + deps + " for dependent test " + dependentTest + " in " + elapsedSeconds + " seconds.");
            } else {
                info("POLLUTER: Found polluter " + deps + " for dependent test " + dependentTest + " in " + elapsedSeconds + " seconds.");
            }

            // Only look for cleaners if the order is not passing; in case of minimizing for setter don't need to look for cleaner
            CleanerData cleanerData;
            if (!expected.equals(Result.PASS)) {
                cleanerData = new CleanerFinder(runner, dependentTest, deps, expected, isolationResult, expectedRun.testOrder()).find();
            } else {
                cleanerData = new CleanerData(dependentTest, expected, isolationResult, new ListEx<CleanerGroup>());
            }

            polluters.add(new PolluterData(operationTime[0], index, deps, cleanerData));
  
            // If not configured to find all, since one is found now, can stop looking
            if (!FIND_ALL) {
                break;
            }

            // A better implementation would remove one by one and not assume polluter groups are mutually exclusive
            order.removeAll(deps);  // Look for other deps besides the ones already found
            index++;
        }
        return index;
    }

    // Returns a list where each element is test from order and the dependent test
    private List<List<String>> getSingleTests(final List<String> order, String dependentTest) {
        List<List<String>> singleTests = new ArrayList<>();
        for (String test : order) {
            if (test.equalsIgnoreCase(dependentTest)) {
                continue;
            }
            singleTests.add(new ArrayList<>(Collections.singletonList(test)));
        }

        return singleTests;
    }

    private List<String> run(List<String> order) throws Exception {
        final List<String> deps = new ArrayList<>();

        if (order.isEmpty()) {
            debug("Order is empty, so it is already minimized!");
            return deps;
        }

        TestMinimizerDeltaDebugger debugger = new TestMinimizerDeltaDebugger(this.runner, this.dependentTest, this.expected);
        deps.addAll(debugger.deltaDebug(order, 2));

        return deps;
    }

    public String getDependentTest() {
        return dependentTest;
    }

    @Override
    public @NonNull Path path() {
        return path;
    }

    @Override
    protected MinimizeTestsResult load() {
        System.out.println("Loading from " + path());

        return new RuntimeThrower<>(() -> new Gson().fromJson(FileUtil.readFile(path()), MinimizeTestsResult.class)).run();
    }

    @Override
    protected void save() {
        new RuntimeThrower<>(() -> {
            Files.createDirectories(path().getParent());
            Files.write(path(), new Gson().toJson(get()).getBytes());

            return null;
        }).run();
    }

    @NonNull
    @Override
    protected MinimizeTestsResult generate() {
        return new RuntimeThrower<>(this::run).run();
    }
}
