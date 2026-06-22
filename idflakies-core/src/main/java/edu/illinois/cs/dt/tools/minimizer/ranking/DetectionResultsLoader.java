package edu.illinois.cs.dt.tools.minimizer.ranking;

import com.google.gson.Gson;
import edu.illinois.cs.testrunner.data.results.Result;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads test-ordering data from iDFlakies' .dtfixingtools/test-runs/results/ directory.
 *
 * <p>Each file in that directory is a serialized TestRunResult produced by the testrunner
 * framework. Its JSON shape is:
 * <pre>
 * {
 *   "id": "...",
 *   "testOrder": ["com.example.A#a", "com.example.B#b", ...],
 *   "results": {
 *     "com.example.A#a": { "name": "...", "result": "PASS", "time": ..., "stackTrace": [] },
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>File names are timestamp-prefixed UUIDs (e.g. {@code 1781558382115-ae26398c-...}), so
 * lexicographic sort gives chronological order.
 */
public class DetectionResultsLoader {

    private static final Gson GSON = new Gson();

    /**
     * Reads TestRunResult JSON files from {@code .dtfixingtools/test-runs/results/} in
     * lexicographic (i.e. chronological) order. Returns at most {@code maxOrders}
     * {@link TestOrderRecord}s. Skips files that are missing, malformed, or have no testOrder.
     *
     * @param dtfixingtoolsDir path to the {@code .dtfixingtools} directory
     * @param targetTest       fully-qualified test name (used by caller; not filtered here)
     * @param maxOrders        maximum number of records to return
     * @return list of parsed records, never null
     * @throws IOException if the directory cannot be listed
     */
    public static List<TestOrderRecord> load(
            Path dtfixingtoolsDir,
            String targetTest,
            int maxOrders) throws IOException {

        Path resultsDir = dtfixingtoolsDir.resolve("test-runs").resolve("results");
        if (!Files.exists(resultsDir)) return Collections.emptyList();

        List<Path> resultFiles;
        try (Stream<Path> stream = Files.list(resultsDir)) {
            resultFiles = stream
                .filter(Files::isRegularFile)
                .sorted((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                .collect(Collectors.toList());
        }

        List<TestOrderRecord> records = new ArrayList<>();
        for (Path f : resultFiles) {
            if (records.size() >= maxOrders) break;
            try {
                TestOrderRecord rec = parse(f);
                if (rec != null) records.add(rec);
            } catch (Exception ignored) {
                // Skip unreadable / malformed files silently
            }
        }
        return records;
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
