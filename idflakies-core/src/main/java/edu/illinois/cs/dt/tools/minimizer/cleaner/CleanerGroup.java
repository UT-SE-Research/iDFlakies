package edu.illinois.cs.dt.tools.minimizer.cleaner;

import java.util.Objects;

import com.reedoei.eunomia.collections.ListEx;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.TimeManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.runner.SmartRunner;

public class CleanerGroup {
    private static final int VERIFY_COUNT = Configuration.config().getProperty("dt.diagnosis.cleaners.verify_count", 1);

    private final String dependentTest;
    private final int originalSize;
    private final ListEx<String> cleanerTests;
    private final int orderFound; // order # this cleaner group was found; used to identify the first cleaner group found
    private TimeManager time;


    public CleanerGroup(final String dependentTest, final int originalSize, final ListEx<String> cleanerTests,
                        int orderFound) {
        this.dependentTest = dependentTest;
        this.originalSize = originalSize;
        this.cleanerTests = cleanerTests;
        this.orderFound = orderFound;
    }

    public int originalSize() {
        return originalSize;
    }

    public boolean confirm(final SmartRunner runner,
                           final ListEx<String> deps,
                           final Result expected, final Result isolationResult, final TimeManager findFilterCandidateTime) throws Exception {

        return OperationTime.runOperation(() -> {

            final ListEx<String> withCleanerOrder = new ListEx<>(deps);
            withCleanerOrder.addAll(cleanerTests);
            withCleanerOrder.add(dependentTest);

            final ListEx<String> withoutCleanerOrder = new ListEx<>(deps);
            withoutCleanerOrder.add(dependentTest);

            for (int i = 0; i < VERIFY_COUNT; i++) {
                System.out.printf("Confirming cleaner group (%d of %d) for %s: %s%n", i, VERIFY_COUNT, dependentTest, cleanerTests);

                if (confirmRun("with", runner, isolationResult, withCleanerOrder)) {
                    return false;
                }

                if (confirmRun("without", runner, expected, withoutCleanerOrder)) {
                    return false;
                }

                final Result withoutCleaner = runner.runList(withoutCleanerOrder).get().results().get(dependentTest).result();

                if (!withoutCleaner.equals(expected)) {
                    return false;
                }
            }

            return true;
        }, (confirmResult, confirmTime) -> {
            this.time = findFilterCandidateTime.manageTime(confirmTime);
            return confirmResult;
        });
    }

    private boolean confirmRun(final String runType,
                               final SmartRunner runner,
                               final Result desiredRes, final ListEx<String> order) {
        System.out.printf("Expected %s cleaner result: %s, got: ", runType, desiredRes);

        final Result res = runner.runList(order).get().results().get(dependentTest).result();

        System.out.println(res);

        return !res.equals(desiredRes);
    }

    public TimeManager time() {
        return time;
    }

    public int orderFound() {
        return orderFound;
    }

    public String dependentTest() {
        return dependentTest;
    }

    public ListEx<String> cleanerTests() {
        return cleanerTests;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependentTest, cleanerTests);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof CleanerGroup) {
            // Don't include original size, we only care about the minimal group for these purposes
            return dependentTest().equals(((CleanerGroup) obj).dependentTest()) &&
                   cleanerTests().equals(((CleanerGroup) obj).cleanerTests());
        }

        return false;
    }

    @Override
    public String toString() {
        return cleanerTests().toString();
    }
}
