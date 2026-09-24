package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import org.junit.Assume;
import org.junit.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankFOScorerTest {
    private static final double DELTA = 1e-9;
    private static final String V = "com.example.VictimTest#test";
    private static final String P = "com.example.PolluterTest#test";
    private static final String N = "com.example.NoiseTest#test";

    private TestOrderRecord order(List<String> tests, Object... pairs) {
        Map<String, Result> results = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            results.put((String) pairs[i], Result.valueOf((String) pairs[i + 1]));
        }
        return new TestOrderRecord(tests, results);
    }

    @Test
    public void polluter_ranks_first_with_verified_scores() {
        TestOrderRecord ord0 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");
        TestOrderRecord ord1 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");
        TestOrderRecord ord2 = order(Arrays.asList(N, V, P), N, "PASS", V, "PASS", P, "PASS");

        RankFOScorer scorer = new RankFOScorer(HeuristicType.METHODS);
        List<ScoredCandidate> ranked = scorer.score(V, Arrays.asList(ord0, ord1, ord2),
            OdType.VICTIM_POLLUTER);

        // P is before V in ord0 and ord1 (both relevant/failing), so it gets a METHODS
        // delta of +1/1 on each of those two orders: +1.0 from ord0 (diffed against the
        // zero baseline) and +1.0 from ord1 (diffed against ord0's rank) = 2.0 total.
        assertEquals(2, ranked.size());
        assertEquals(P, ranked.get(0).getTestName());
        assertEquals(2.0, ranked.get(0).getPolluterScore(), DELTA);
        assertEquals(0.0, ranked.get(0).getNonPolluterScore(), DELTA);
        assertEquals(N, ranked.get(1).getTestName());
        assertEquals(0.0, ranked.get(1).getPolluterScore(), DELTA);
        assertEquals(1.0, ranked.get(1).getNonPolluterScore(), DELTA);
    }

    @Test
    public void empty_orderings_returns_empty_list() {
        RankFOScorer scorer = new RankFOScorer(HeuristicType.PLUS_ONE);
        assertTrue(scorer.score(V, Arrays.asList(), OdType.VICTIM_POLLUTER).isEmpty());
    }

    @Test
    public void victim_not_in_any_ordering_returns_empty_list() {
        TestOrderRecord ord = order(Arrays.asList(P, N), P, "PASS", N, "PASS");
        RankFOScorer scorer = new RankFOScorer(HeuristicType.PLUS_ONE);
        assertTrue(scorer.score(V, Arrays.asList(ord), OdType.VICTIM_POLLUTER).isEmpty());
    }

    @Test
    public void single_relevant_ordering_still_produces_a_signal() {
        // Only one ordering total, and the candidate appears before a FAILING victim.
        // The Python reference (getRankedLists/generateClassScoreVP in getCombinations.py)
        // seeds an explicit order "-1" baseline of zeros and diffs every real order
        // (including order 0) against its predecessor. So a single relevant ordering
        // must already contribute a nonzero polluterScore -- it is not "no signal yet".
        TestOrderRecord ord = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");
        RankFOScorer scorer = new RankFOScorer(HeuristicType.PLUS_ONE);
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord), OdType.VICTIM_POLLUTER);
        assertEquals(1, result.size());
        assertEquals(1.0, result.get(0).getPolluterScore(), DELTA);
        assertEquals(0.0, result.get(0).getNonPolluterScore(), DELTA);
    }

    @Test
    public void bss_type_orderIsRelevant_when_brittle_passes() {
        TestOrderRecord ord0 = order(Arrays.asList(P, V), P, "PASS", V, "PASS");
        TestOrderRecord ord1 = order(Arrays.asList(P, V), P, "PASS", V, "PASS");

        RankFOScorer scorer = new RankFOScorer(HeuristicType.PLUS_ONE);
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord0, ord1),
            OdType.BRITTLE_STATESETTER);

        // Both orderings are relevant (BSS: brittle V passes); P is before V in each,
        // contributing +1.0 from ord0 (vs. zero baseline) and +1.0 from ord1 = 2.0 total.
        assertEquals(1, result.size());
        assertEquals(P, result.get(0).getTestName());
        assertEquals(2.0, result.get(0).getPolluterScore(), DELTA);
    }

    @Test
    public void loader_parses_real_dtfixingtools_without_throwing() throws Exception {
        Path dtDir = Paths.get(
            "/media/iit/01DAF7B03B5CE760/UIUC++/testProjects/http-request/lib/.dtfixingtools"
        );
        Assume.assumeTrue(
            "Skip: run mvn idflakies:detect on http-request/ first",
            dtDir.toFile().exists()
        );
        List<TestOrderRecord> records = DetectionResultsLoader.load(dtDir, "dummy.Target#test", 5);
        assertTrue("load() must return non-null", records != null);
    }

    @Test
    public void maxOrders_cap_limits_orderings_processed() {
        List<TestOrderRecord> orderings = Arrays.asList(
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE")
        );
        RankFOScorer scorer = new RankFOScorer(HeuristicType.PLUS_ONE, 2);
        List<ScoredCandidate> result = scorer.score(V, orderings, OdType.VICTIM_POLLUTER);
        // Only the first 2 of the 5 orderings are processed (cap=2): +1.0 from order 0
        // (vs. zero baseline) and +1.0 from order 1 = 2.0, regardless of orders 2-4.
        assertEquals(2.0, result.get(0).getPolluterScore(), DELTA);
    }

    // ── Combined (+1, D) ──────────────────────────────────────────────────

    @Test
    public void combined_plus_one_distance_breaks_ties_by_closeness() {
        // [N, P, V] failing: Plus One gives both N and P the same polluterScore.
        // Distance tiebreaker from last ordering: P (idx=1, dist=1) < N (idx=0, dist=2)
        // → P ranks first (closer to victim).
        TestOrderRecord ord0 = order(Arrays.asList(N, P, V), N, "PASS", P, "PASS", V, "FAILURE");
        TestOrderRecord ord1 = order(Arrays.asList(N, P, V), N, "PASS", P, "PASS", V, "FAILURE");

        RankFOScorer scorer = new RankFOScorer(HeuristicType.COMBINED_PLUS_ONE_DISTANCE);
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord0, ord1),
            OdType.VICTIM_POLLUTER);

        assertEquals(2, result.size());
        assertEquals("P (closer, dist=1) must rank first", P, result.get(0).getTestName());
        assertEquals("N (farther, dist=2) must rank second", N, result.get(1).getTestName());
    }

    // ── Combined (#M, D) ──────────────────────────────────────────────────

    @Test
    public void combined_methods_distance_breaks_ties_by_closeness() {
        // Same setup: #Methods gives both 1/2 → tied → Distance tiebreaker: P closer.
        TestOrderRecord ord0 = order(Arrays.asList(N, P, V), N, "PASS", P, "PASS", V, "FAILURE");
        TestOrderRecord ord1 = order(Arrays.asList(N, P, V), N, "PASS", P, "PASS", V, "FAILURE");

        RankFOScorer scorer = new RankFOScorer(HeuristicType.COMBINED_METHODS_DISTANCE);
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord0, ord1),
            OdType.VICTIM_POLLUTER);

        assertEquals(2, result.size());
        assertEquals("P (closer, dist=1) must rank first", P, result.get(0).getTestName());
        assertEquals("N (farther, dist=2) must rank second", N, result.get(1).getTestName());
    }

    @Test
    public void combined_plus_one_distance_no_tie_preserves_primary_order() {
        // P always before V; N never before V → P has higher polluterScore, no tie needed.
        TestOrderRecord ord0 = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");
        TestOrderRecord ord1 = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");

        RankFOScorer scorer = new RankFOScorer(HeuristicType.COMBINED_PLUS_ONE_DISTANCE);
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord0, ord1),
            OdType.VICTIM_POLLUTER);

        // P is before V in both relevant orderings: +1.0 from ord0 (vs. zero baseline)
        // and +1.0 from ord1 = 2.0 total.
        assertEquals(1, result.size());
        assertEquals(P, result.get(0).getTestName());
        assertEquals(2.0, result.get(0).getPolluterScore(), DELTA);
    }
}
