package edu.illinois.cs.dt.tools.detection.classifiers;

import com.reedoei.eunomia.collections.ListUtil;
import edu.illinois.cs.dt.tools.runner.data.TestRun;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NonorderClassifier implements Classifier {
    private final Set<String> flaky = new HashSet<>();
    private final Map<String, Map<String, TestRun>> knownRuns = new HashMap<>();

    @Override
    public void update(final TestRunResult testRunResult) {
        testRunResult.results().forEach((testName, result) -> {
            // If we already know it's non-order dependent, just leave
            if (flaky.contains(testName)) {
                return;
            }

            final Map<String, TestRun> runs = knownRuns.getOrDefault(testName, new HashMap<>());
            final String orderHash = MD5.hashOrder(ListUtil.before(testRunResult.testOrder(), testName));

            final TestRun expectedResult = runs.get(orderHash);

            if (expectedResult != null) {
                // If the order is the same, and the results don't match, must be non-order dependent
                if (!result.result().equals(expectedResult.result())) {
                    flaky.add(testName);
                }
            } else {
                runs.put(orderHash, new TestRun(testRunResult.testOrder(), result.result(), testRunResult.id()));
                knownRuns.put(testName, runs);
            }
        });
    }

    public Set<String> nonorderTests() {
        return flaky;
    }

    @Override
    public void close() throws Exception {
        flaky.clear();;
        knownRuns.clear();
    }
}
