package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.TestRun;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.coreplugin.TestPluginUtil;
import edu.illinois.cs.testrunner.runner.Runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DetectorUtil {
    public static TestRunResult originalResults(final List<String> originalOrder, final Runner runner) {
        final int originalOrderTries = Configuration.config().getProperty("dt.detector.original_order.retry_count", 3);
        final boolean allMustPass = Configuration.config().getProperty("dt.detector.original_order.all_must_pass", true);

        System.out.println("[INFO] Getting original results (" + originalOrder.size() + " tests).");

        TestRunResult origResult = null;

        boolean allPassing = false;
        // Try to run it three times, to see if we can get everything to pass (except for ignored tests)
        for (int i = 0; i < originalOrderTries; i++) {
            origResult = runner.runList(originalOrder).get();

            try {
                Files.write(DetectorPathManager.originalResultsLog(), (origResult.id() + "\n").getBytes(),
                        Files.exists(DetectorPathManager.originalResultsLog()) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (IOException ignored) {}

            if (allPass(origResult)) {
                allPassing = true;
                break;
            }
        }

        if (!allPassing) {
            if (allMustPass) {
                throw new NoPassingOrderException("No passing order for tests (" + originalOrderTries + " runs)");
            } else {
                TestPluginUtil.project.info("No passing order for tests (" + originalOrderTries + " runs). Continuing anyway with last run.");
            }
        }

        return origResult;
    }

    public static boolean allPass(final TestRunResult testRunResult) {
        return testRunResult.results().values().stream()
                // Ignored tests will show up as SKIPPED, but that's fine because surefire would've skipped them too
                .allMatch(tr -> tr.result().equals(Result.PASS) || tr.result().equals(Result.SKIPPED));
    }

    private static <T> List<T> before(final List<T> ts, final T t) {
        final int i = ts.indexOf(t);

        if (i != -1) {
            return new ArrayList<>(ts.subList(0, Math.min(ts.size(), i)));
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DependentTest> flakyTests(final TestRunResult intended,
                                                 final TestRunResult revealed,
                                                 final boolean onlyFirstFailure) {
        final List<DependentTest> result = new ArrayList<>();

        for (final Map.Entry<String, TestResult> entry : intended.results().entrySet()) {
            final String testName = entry.getKey();
            final TestResult intendedResult = entry.getValue();
            final Map<String, TestResult> revealedResults = revealed.results();

            if (revealedResults.containsKey(testName)) {
                final Result revealedResult = revealedResults.get(testName).result();
                if (!revealedResult.equals(intendedResult.result())) {
                    result.add(new DependentTest(testName,
                            new TestRun(before(intended.testOrder(), testName), intendedResult.result(), intended.id()),
                            new TestRun(before(revealed.testOrder(), testName), revealedResult, revealed.id())));

                    if (onlyFirstFailure) {
                        // Only keep the first failure, if any
                        break;
                    }
                }
            }
        }
        return result;
    }
}
