package edu.illinois.cs.dt.tools.minimizer.ranking;

import com.google.gson.Gson;
import edu.illinois.cs.testrunner.data.results.Result;
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
 * Loads test-ordering data from iDFlakies' .dtfixingtools directory.
 *
 * <p>The loader follows two-level indirection:
 * <ol>
 *   <li>Read {@code detection-results/random-class-method/roundN.json} files (sorted by round
 *       number) to get the {@code testRunIds} that belong to a detection run.</li>
 *   <li>For each {@code testRunId}, look up the corresponding file in
 *       {@code test-runs/results/} and parse it as a {@link TestOrderRecord}.</li>
 * </ol>
 * Only orderings in which {@code targetTest} was actually run are returned.
 */
public class DetectionResultsLoader {

    private static final Gson GSON = new Gson();

    /**
     * Returns at most {@code maxOrders} {@link TestOrderRecord}s that contain {@code targetTest},
     * sourced exclusively from the detection rounds recorded in
     * {@code detection-results/random-class-method/roundN.json}.
     *
     * @param dtfixingtoolsDir path to the {@code .dtfixingtools} directory
     * @param targetTest       fully-qualified test name; only orderings containing this test are returned
     * @param maxOrders        maximum number of records to return
     * @return list of parsed records, never null
     * @throws IOException if a directory cannot be listed
     */
    public static List<TestOrderRecord> load(
            Path dtfixingtoolsDir,
            String targetTest,
            int maxOrders) throws IOException {

        // Step 1: collect testRunIds from detection round files
        Path detectionDir = dtfixingtoolsDir
                .resolve("detection-results")
                .resolve("random-class-method");
        if (!Files.exists(detectionDir)) return Collections.emptyList();

        List<Path> roundFiles;
        try (Stream<Path> stream = Files.list(detectionDir)) {
            roundFiles = stream
                .filter(p -> p.getFileName().toString().matches("round\\d+\\.json"))
                .sorted(Comparator.comparingInt(p -> roundNumber(p.getFileName().toString())))
                .collect(Collectors.toList());
        }
        if (roundFiles.isEmpty()) return Collections.emptyList();

        List<String> testRunIds = new ArrayList<>();
        for (Path roundFile : roundFiles) {
            try (FileReader reader = new FileReader(roundFile.toFile())) {
                DetectionRoundJson round = GSON.fromJson(reader, DetectionRoundJson.class);
                if (round != null && round.testRunIds != null) {
                    testRunIds.addAll(round.testRunIds);
                }
            } catch (Exception ignored) {}
        }
        if (testRunIds.isEmpty()) return Collections.emptyList();

        // Step 2: load and filter by targetTest
        Path resultsDir = dtfixingtoolsDir.resolve("test-runs").resolve("results");
        if (!Files.exists(resultsDir)) return Collections.emptyList();

        List<TestOrderRecord> records = new ArrayList<>();
        for (String runId : testRunIds) {
            if (records.size() >= maxOrders) break;
            Path resultFile = resultsDir.resolve(runId);
            if (!Files.exists(resultFile)) continue;
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

    /**
     * Parses a single TestRunResult JSON file into a {@link TestOrderRecord}.
     * Package-private for unit testing.
     */
    static TestOrderRecord parse(Path file) throws IOException {
        try (FileReader reader = new FileReader(file.toFile())) {
            RoundResultJson raw = GSON.fromJson(reader, RoundResultJson.class);
            if (raw == null || raw.testOrder == null) return null;

            Map<String, Result> results = new HashMap<>();
            if (raw.results != null) {
                for (Map.Entry<String, TestResultJson> entry : raw.results.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().result != null) {
                        try {
                            results.put(entry.getKey(),
                                Result.valueOf(entry.getValue().result));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown result string (e.g. future enum value) — skip entry
                        }
                    }
                }
            }
            return new TestOrderRecord(raw.testOrder, results);
        }
    }

    // detection-results/random-class-method/roundN.json structure.
    private static class DetectionRoundJson {
        List<String> testRunIds;
    }

    // Mirrors TestRunResult JSON structure confirmed from .dtfixingtools/test-runs/results/ files.
    // Top-level fields: id (ignored), testOrder, results.
    private static class RoundResultJson {
        List<String> testOrder;
        Map<String, TestResultJson> results;
    }

    // Mirrors TestResult JSON structure: name (ignored), result, time (ignored), stackTrace (ignored).
    private static class TestResultJson {
        String result; // e.g. "PASS", "FAILURE", "ERROR", "SKIPPED"
    }
}
