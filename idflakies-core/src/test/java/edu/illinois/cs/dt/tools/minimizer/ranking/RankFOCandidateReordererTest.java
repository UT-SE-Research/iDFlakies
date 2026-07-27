package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankFOCandidateReordererTest {

    private static final String V = "com.example.VictimTest#test";
    private static final String P = "com.example.PolluterTest#test";
    private static final String N = "com.example.NoiseTest#test";
    private static final String X = "com.example.ExtraTest#test";

    private TestOrderRecord order(List<String> tests, Object... pairs) {
        Map<String, Result> results = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            results.put((String) pairs[i], Result.valueOf((String) pairs[i + 1]));
        }
        return new TestOrderRecord(tests, results);
    }

    /**
     * Calls reorder() using the package-private scoring path directly (bypasses PathManager)
     * by delegating to a helper that accepts pre-built TestOrderRecords.
     */
    private List<String> reorderWith(List<String> prefix, String target, Result isolation,
                                     List<TestOrderRecord> orderings) {
        OdType odType = (isolation == Result.PASS)
            ? OdType.VICTIM_POLLUTER
            : OdType.BRITTLE_STATESETTER;

        String heuristicName = System.getProperty("dt.rankfo.heuristic", "DISTANCE");
        HeuristicType hType = HeuristicType.valueOf(heuristicName);

        List<ScoredCandidate> ranked =
            new RankFOScorer(hType).score(target, orderings, odType);

        Map<String, Double> scoreMap = new HashMap<>();
        for (ScoredCandidate sc : ranked) {
            scoreMap.put(sc.getTestName(), sc.getPolluterScore());
        }

        List<String> reordered = new ArrayList<>(prefix);
        reordered.sort(Comparator.comparingDouble(
            (String t) -> scoreMap.getOrDefault(t, Double.NEGATIVE_INFINITY)
        ).reversed());
        return reordered;
    }

    @Test
    public void polluter_moves_to_front_when_consistently_before_victim_in_failing_rounds() {
        // P appears before V in two failing rounds; N is after V so it is never a candidate.
        // N therefore gets NEGATIVE_INFINITY from the score lookup and sorts after P.
        TestOrderRecord ord0 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");
        TestOrderRecord ord1 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");

        List<String> prefix = Arrays.asList(N, P); // N first in original order
        List<String> result = reorderWith(prefix, V, Result.PASS, Arrays.asList(ord0, ord1));

        assertEquals("Polluter must be ranked first", P, result.get(0));
        assertEquals("Noise must be ranked second", N, result.get(1));
    }

    @Test
    public void unscored_tests_preserved_at_end_in_original_relative_order() {
        // X never appears in any ordering; P does
        TestOrderRecord ord0 = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");
        TestOrderRecord ord1 = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");

        // Original prefix: [X, N, P] — X and N are unscored
        List<String> prefix = Arrays.asList(X, N, P);
        List<String> result = reorderWith(prefix, V, Result.PASS, Arrays.asList(ord0, ord1));

        assertEquals("Scored candidate P must be first", P, result.get(0));
        // X and N are unscored; their relative order from prefix must be preserved
        assertEquals(3, result.size());
        int xIdx = result.indexOf(X);
        int nIdx = result.indexOf(N);
        assertTrue("X must appear after P (unscored)", xIdx > 0);
        assertTrue("N must appear after P (unscored)", nIdx > 0);
        assertTrue("X must come before N (original relative order preserved)", xIdx < nIdx);
    }

    @Test
    public void empty_orderings_returns_prefix_unchanged() {
        List<String> prefix = Arrays.asList(N, P, X);
        List<String> result = reorderWith(prefix, V, Result.PASS, Collections.emptyList());
        // With empty orderings, scorer returns empty → all unscored → original order preserved
        assertEquals(prefix, result);
    }

    @Test
    public void single_ordering_produces_zero_class_scores_so_order_is_stable() {
        // With only one ordering, no consecutive pair → polluterScore=0 for all.
        // reorderWith() uses a stable sort by polluterScore, so prefix order is preserved.
        TestOrderRecord ord0 = order(Arrays.asList(P, N, V), P, "PASS", N, "PASS", V, "FAILURE");
        List<String> prefix = Arrays.asList(N, P);
        List<String> result = reorderWith(prefix, V, Result.PASS, Collections.singletonList(ord0));
        // Stable sort on equal scores preserves prefix order [N, P]
        assertEquals(Arrays.asList(N, P), result);
    }

    @Test
    public void bss_type_reorders_statesetter_to_front() {
        // Brittle test: passes in isolation → BRITTLE_STATESETTER
        // Setter S appears before brittle B in passing rounds
        String S = "com.example.SetterTest#test";
        String B = "com.example.BrittleTest#test";
        // N is after B so it is never a candidate; S gets polluterScore=1.0, N gets NEGATIVE_INFINITY.
        TestOrderRecord ord0 = order(Arrays.asList(S, B, N), S, "PASS", B, "PASS", N, "PASS");
        TestOrderRecord ord1 = order(Arrays.asList(S, B, N), S, "PASS", B, "PASS", N, "PASS");

        List<String> prefix = Arrays.asList(N, S);
        // isolationResult != PASS → BRITTLE_STATESETTER (B fails alone, passes after setter)
        List<String> result = reorderWith(prefix, B, Result.FAILURE, Arrays.asList(ord0, ord1));

        assertEquals("Setter must be ranked first", S, result.get(0));
    }

    // ── cache integration ─────────────────────────────────────────────────

    private Path tmpDir;

    @Before
    public void setUpTmpDir() throws IOException {
        tmpDir = Files.createTempDirectory("rankfo-reorderer-cache-test");
        Path resultsDir = tmpDir.resolve("test-runs").resolve("results");
        Files.createDirectories(resultsDir);
        // Old mtime so cached entries are fresh
        Files.setLastModifiedTime(resultsDir, FileTime.fromMillis(System.currentTimeMillis() - 10_000));
    }

    @After
    public void tearDownTmpDir() throws IOException {
        if (tmpDir != null) {
            Files.walk(tmpDir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    @Test
    public void reorder_usesCachedScores_whenCacheIsValid() throws IOException {
        // Pre-populate cache: P has a high polluter score
        List<ScoredCandidate> cached = Arrays.asList(
                new ScoredCandidate(P, 10.0, -1.0),
                new ScoredCandidate(N, 0.0, 0.0));
        RankFOScoreCache.save(tmpDir, V, HeuristicType.DISTANCE,
                OdType.VICTIM_POLLUTER, cached);

        // No real detection results exist in tmpDir — must use cache
        List<String> prefix = Arrays.asList(N, P);
        List<String> result = RankFOCandidateReorderer.reorder(prefix, V, Result.PASS, tmpDir);

        assertEquals("P should be first because cache ranks it highest", P, result.get(0));
        assertEquals("N should be second", N, result.get(1));
    }

    @Test
    public void reorder_fallsBackToOriginalOrder_whenCacheAbsentAndNoDetectionResults() {
        // tmpDir has results dir but no files, no cache
        List<String> prefix = Arrays.asList(N, P, X);
        List<String> result = RankFOCandidateReorderer.reorder(prefix, V, Result.PASS, tmpDir);

        assertEquals("Prefix must be returned unchanged when no data available", prefix, result);
    }

    @Test
    public void reorder_writesCache_afterComputingFromDetectionResults() throws IOException {
        // DetectionResultsLoader now reads round files from detection-results/random-class-method/
        // and follows testRunIds to test-runs/results/.  Build that structure:
        //   detection-results/random-class-method/round0.json  → {"testRunIds":["run0","run1"]}
        //   test-runs/results/run0                             → ordering with P,V,N
        //   test-runs/results/run1                             → ordering with P,V,N
        Path detectionDir = tmpDir.resolve("detection-results").resolve("random-class-method");
        Files.createDirectories(detectionDir);
        Files.write(detectionDir.resolve("round0.json"),
                "{\"testRunIds\":[\"run0\",\"run1\"]}".getBytes());

        Path resultsDir = tmpDir.resolve("test-runs").resolve("results");
        Files.createDirectories(resultsDir);
        writeDetectionResult(resultsDir, "run0", Arrays.asList(P, V, N),
                P, "PASS", V, "FAILURE", N, "PASS");
        writeDetectionResult(resultsDir, "run1", Arrays.asList(P, V, N),
                P, "PASS", V, "FAILURE", N, "PASS");
        // Mark results dir as recently modified so cache is considered stale
        Files.setLastModifiedTime(resultsDir, FileTime.fromMillis(System.currentTimeMillis() - 1_000));

        System.setProperty("dt.rankfo.heuristic", "DISTANCE");
        try {
            RankFOCandidateReorderer.reorder(Arrays.asList(N, P), V, Result.PASS, tmpDir);
        } finally {
            System.clearProperty("dt.rankfo.heuristic");
        }

        // Cache file should now exist
        Path cacheDir = tmpDir.resolve("rankfo-scores");
        assertTrue("Cache directory should be created after scoring", Files.isDirectory(cacheDir));
        assertTrue("At least one cache file should be written",
                Files.list(cacheDir).findAny().isPresent());
    }

    private void writeDetectionResult(Path dir, String filename, List<String> order,
                                      Object... resultPairs) throws IOException {
        // test-runs/results files have no extension; format: {"testOrder":[...],"results":{...}}
        StringBuilder sb = new StringBuilder();
        sb.append("{\"testOrder\":[");
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(order.get(i)).append("\"");
        }
        sb.append("],\"results\":{");
        for (int i = 0; i < resultPairs.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(resultPairs[i]).append("\":{\"result\":\"")
              .append(resultPairs[i + 1]).append("\"}");
        }
        sb.append("}}");
        Files.write(dir.resolve(filename), sb.toString().getBytes());
    }

}
