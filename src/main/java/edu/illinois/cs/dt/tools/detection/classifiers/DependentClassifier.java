package edu.illinois.cs.dt.tools.detection.classifiers;

import edu.illinois.cs.dt.tools.runner.data.TestRun;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependentClassifier implements Classifier {
    private final Map<String, Set<Result>> knownResults = new HashMap<>();
    private final Map<String, Set<TestRun>> dependentRuns = new HashMap<>();
    private final boolean skipAfterFirstFailure;

    public DependentClassifier(final boolean skipAfterFirstFailure) {
        this.skipAfterFirstFailure = skipAfterFirstFailure;
    }

    @Override
    public void update(final TestRunResult testRunResult) {
        for (final Map.Entry<String, TestResult> entry : testRunResult.results().entrySet()) {
            final String testName = entry.getKey();
            final TestResult result = entry.getValue();

            final Set<Result> results = knownResults.getOrDefault(testName, new HashSet<>());

            if (!results.contains(result.result())) {
                results.add(result.result());

                final Set<TestRun> runs = dependentRuns.getOrDefault(testName, new HashSet<>());
                runs.add(new TestRun(testRunResult.testOrder(), result.result(), testRunResult.id()));
                dependentRuns.put(testName, runs);
            }

            knownResults.put(testName, results);

            // Skip everything after the first failure because it's unreliable
            if (skipAfterFirstFailure) {
                if (!result.result().equals(Result.PASS)) {
                    break;
                }
            }
        }
    }

    public Map<String, Set<TestRun>> dependentRuns() {
        return dependentRuns;
    }

    public Set<String> dependentTests(final Set<String> nonorderTests) {
        return knownResults.keySet().stream()
                .filter(t -> knownResults.get(t).size() > 1)
                .filter(t -> !nonorderTests.contains(t))
                .collect(Collectors.toSet());
    }

    @Override
    public void close() throws Exception {
        knownResults.clear();
        dependentRuns.clear();
    }
}
