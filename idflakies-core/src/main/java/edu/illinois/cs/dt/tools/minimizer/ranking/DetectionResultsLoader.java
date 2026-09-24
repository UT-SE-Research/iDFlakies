package edu.illinois.cs.dt.tools.minimizer.ranking;

import com.google.gson.Gson;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads historical test-ordering records from a project's .dtfixingtools directory, for
 * RankFO to score. The on-disk layout has two levels of indirection, both written by
 * iDFlakies' own detector (not by this class):
 *
 *   detection-results/random-class-method/round&lt;N&gt;.json -- one per detection round,
 *     holding the list of testRunIds executed in that round.
 *   test-runs/results/&lt;testRunId&gt;                       -- one per test run, holding
 *     the full order and per-test results for that run.
 *
 * Round files are read in round-number order, not filesystem-listing order (see the
 * roundNumber()-based sort below) -- RankFOScorer depends on this: (1) when there are more
 * historical rounds than maxOrders, only the first maxOrders by round number are used, and
 * that selection must be deterministic rather than filesystem-order-dependent; (2) the
 * COMBINED_* heuristics break ties using the candidate's distance in the *last* observed
 * order, which is only meaningful if "last" means chronologically last.
 */
public class DetectionResultsLoader {

    private static final Gson GSON = new Gson();

    /**
     * @param dtfixingtoolsDir the project's .dtfixingtools directory
     * @param targetTest       fully-qualified name of the victim/target test being scored
     * @param maxOrders        maximum number of historical orderings to return
     */
    public static List<TestOrderRecord> load(
            Path dtfixingtoolsDir,
            String targetTest,
            int maxOrders) throws IOException {

        Path detectionDir = dtfixingtoolsDir
                .resolve("detection-results")
                .resolve("random-class-method");
        if (!Files.exists(detectionDir)) {
            return Collections.emptyList();
        }

        List<Path> roundFiles;
        try (Stream<Path> stream = Files.list(detectionDir)) {
            roundFiles = stream
                .filter(p -> p.getFileName().toString().matches("round\\d+\\.json"))
                .sorted(Comparator.comparingInt(p -> roundNumber(p.getFileName().toString())))
                .collect(Collectors.toList());
        }
        if (roundFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> testRunIds = new ArrayList<>();
        for (Path roundFile : roundFiles) {
            try (FileReader reader = new FileReader(roundFile.toFile())) {
                DetectionRound round = GSON.fromJson(reader, DetectionRound.class);
                if (round != null && round.testRunIds() != null) {
                    testRunIds.addAll(round.testRunIds());
                }
            } catch (Exception ignored) {}
        }
        if (testRunIds.isEmpty()) {
            return Collections.emptyList();
        }

        Path resultsDir = dtfixingtoolsDir.resolve("test-runs").resolve("results");
        if (!Files.exists(resultsDir)) {
            return Collections.emptyList();
        }

        List<TestOrderRecord> records = new ArrayList<>();
        for (String runId : testRunIds) {
            if (records.size() >= maxOrders) {
                break;
            }
            Path resultFile = resultsDir.resolve(runId);
            if (!Files.exists(resultFile)) {
                continue;
            }
            try {
                TestOrderRecord rec = parse(resultFile);
                if (rec != null && rec.getResult(targetTest) != null) {
                    records.add(rec);
                }
            } catch (Exception ignored) {}
        }
        return records;
    }

    private static int roundNumber(String filename) {
        try {
            return Integer.parseInt(filename.replace("round", "").replace(".json", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    static TestOrderRecord parse(Path file) throws IOException {
        try (FileReader reader = new FileReader(file.toFile())) {
            TestRunResult raw = GSON.fromJson(reader, TestRunResult.class);
            if (raw == null || raw.testOrder() == null) {
                return null;
            }

            Map<String, Result> results = new HashMap<>();
            if (raw.results() != null) {
                for (Map.Entry<String, TestResult> entry : raw.results().entrySet()) {
                    if (entry.getValue() != null && entry.getValue().result() != null) {
                        results.put(entry.getKey(), entry.getValue().result());
                    }
                }
            }
            return new TestOrderRecord(raw.testOrder(), results);
        }
    }
}
