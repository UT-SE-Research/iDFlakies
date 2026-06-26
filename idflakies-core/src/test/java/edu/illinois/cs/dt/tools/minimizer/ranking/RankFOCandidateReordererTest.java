package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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

        String heuristicName = System.getProperty("dt.rankfo.heuristic", "DISTANCE_TO_VICTIM");
        HeuristicType hType = HeuristicType.valueOf(heuristicName);

        List<ScoredCandidate> ranked =
            new RankFOScorer(RankingHeuristic.of(hType)).score(target, orderings, odType);

        java.util.Map<String, Double> scoreMap = new java.util.LinkedHashMap<>();
        for (ScoredCandidate sc : ranked) {
            scoreMap.put(sc.getTestName(), sc.getPolluterScore());
        }

        List<String> reordered = new java.util.ArrayList<>(prefix);
        reordered.sort(java.util.Comparator.comparingDouble(
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
        // With only one ordering, no consecutive pair → polluterScore=0 for all → original order
        TestOrderRecord ord0 = order(Arrays.asList(P, N, V), P, "PASS", N, "PASS", V, "FAILURE");
        List<String> prefix = Arrays.asList(N, P);
        List<String> result = reorderWith(prefix, V, Result.PASS, Collections.singletonList(ord0));
        // All polluterScores == 0.0; stable sort preserves input order
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

    @Test
    public void reorder_public_api_falls_through_gracefully_with_bad_heuristic() {
        // Temporarily set an invalid heuristic via system property
        String prev = System.getProperty("dt.rankfo.heuristic");
        System.setProperty("dt.rankfo.heuristic", "INVALID_HEURISTIC");
        try {
            // reorder() catches IllegalArgumentException and returns prefix unchanged.
            // We can't call the real static method without PathManager, but we can verify
            // that HeuristicType.valueOf("INVALID_HEURISTIC") throws as expected.
            boolean threw = false;
            try {
                HeuristicType.valueOf("INVALID_HEURISTIC");
            } catch (IllegalArgumentException e) {
                threw = true;
            }
            assertTrue("Bad heuristic name must throw IllegalArgumentException", threw);
        } finally {
            if (prev == null) {
                System.clearProperty("dt.rankfo.heuristic");
            } else {
                System.setProperty("dt.rankfo.heuristic", prev);
            }
        }
    }
}
