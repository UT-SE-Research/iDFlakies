package edu.illinois.cs.dt.tools.detection;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.StandardMain;
import edu.illinois.cs.dt.tools.analysis.ResultDirVisitor;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RebuildDetectionRounds extends StandardMain {
    private final Path results;

    private RebuildDetectionRounds(final String[] args) {
        super(args);

        results = Paths.get(getArgRequired("results"));
    }

    public static void main(final String[] args) {
        try {
            new RebuildDetectionRounds(args).run();
        } catch (Exception e) {
            e.printStackTrace();

            System.exit(1);
        }

        System.exit(0);
    }

    @Override
    protected void run() throws Exception {
        final ListEx<Path> allResultsFolders = new ListEx<>();
        Files.walkFileTree(results, new ResultDirVisitor(allResultsFolders));

        for (int i = 0; i < allResultsFolders.size(); i++) {
            final Path resultsFolder = allResultsFolders.get(i);
            System.out.println("[INFO] Rebuilding rounds in " + resultsFolder + " (" + i + " of " + allResultsFolders.size() + ")");

            final Path testRuns = resultsFolder.resolve(RunnerPathManager.TEST_RUNS);
            final Path originalOrderPath = resultsFolder.resolve(DetectorPathManager.ORIGINAL_ORDER);
            final Path detectionResults = resultsFolder.resolve(DetectorPathManager.DETECTION_RESULTS);

            if (!Files.exists(originalOrderPath)) {
                System.out.println("No original order found at: " + originalOrderPath);
                continue;
            }

            if (!Files.exists(testRuns)) {
                System.out.println("No test runs found at: " + testRuns);
                continue;
            }

            if (!Files.exists(detectionResults)) {
                System.out.println("No detection results found at: " + detectionResults);
                continue;
            }

            final List<String> originalOrder = Files.readAllLines(originalOrderPath);
            final TestRunResult originalResults = makeOriginalResults(originalOrder);

            // To make sure we don't refind flaky tests
            final Set<String> knownFlaky = rebuildRounds(originalOrder, originalResults, resultsFolder, "flaky", new HashSet<>());
            rebuildRounds(originalOrder, originalResults, resultsFolder, "random", knownFlaky);
            rebuildRounds(originalOrder, originalResults, resultsFolder, "random-class", knownFlaky);
            rebuildRounds(originalOrder, originalResults, resultsFolder, "reverse", knownFlaky);
            rebuildRounds(originalOrder, originalResults, resultsFolder, "reverse-class", knownFlaky);
            rebuildRounds(originalOrder, originalResults, resultsFolder, "smart-shuffle", knownFlaky);
        }
    }

    /**
     * This method creates a fake TestRunResult object, consisting of all passing results.
     * This is fine because we ensure that all tests pass in the original order before running any detection.
     */
    private TestRunResult makeOriginalResults(final List<String> originalOrder) {
        final Map<String, TestResult> testResults = new HashMap<>();

        for (final String testName : originalOrder) {
            testResults.put(testName, new TestResult(testName, Result.PASS, 0, new StackTraceElement[0]));
        }

        return new TestRunResult("id doesnt matter", originalOrder, testResults);
    }

    private Set<String> rebuildRounds(final List<String> originalOrder,
                                      final TestRunResult originalResults,
                                      final Path resultsPath,
                                      final String roundType,
                                      final Set<String> knownFlaky) throws IOException {
        final Set<String> newKnownFlaky = new HashSet<>(knownFlaky);

        if (!Files.exists(resultsPath.resolve(roundType))) {
            return newKnownFlaky;
        }

        try (final Stream<Path> list = Files.list(resultsPath.resolve(DetectorPathManager.DETECTION_RESULTS).resolve(roundType))) {
            final List<Path> roundPaths = list.collect(Collectors.toList());

            for (final Path roundPath : roundPaths) {
                final DetectionRound detectionRound = new Gson().fromJson(FileUtil.readFile(roundPath), DetectionRound.class);

                final List<DependentTest> allDts = new ArrayList<>();

                for (final String testRunId : detectionRound.testRunIds()) {
                    allDts.addAll(dtsInTestRun(originalResults, resultsPath, newKnownFlaky, testRunId));
                }

                final DetectionRound newRound = new DetectionRound(detectionRound.testRunIds(), allDts, allDts, detectionRound.roundTime());

                Files.write(roundPath, new Gson().toJson(newRound).getBytes());
            }
        }

        return newKnownFlaky;
    }

    private List<DependentTest> dtsInTestRun(final TestRunResult originalResults, final Path resultsPath,
                                             final Set<String> knownFlaky, final String testRunId)
            throws IOException {
        final TestRunResult testRunResult = readTestRunResult(resultsPath, testRunId);
        final List<DependentTest> dependentTests =
                DetectorUtil.flakyTests(originalResults, testRunResult, false);

        dependentTests.removeIf(dt -> knownFlaky.contains(dt.name()));
        for (final DependentTest dependentTest : dependentTests) {
            knownFlaky.add(dependentTest.name());
        }

        return dependentTests;
    }

    private TestRunResult readTestRunResult(final Path resultsPath, final String id) throws IOException {
        final String contents = FileUtil.readFile(resultsPath.resolve(RunnerPathManager.TEST_RUNS).resolve("results").resolve(id));

        return new Gson().fromJson(contents, TestRunResult.class);
    }
}
