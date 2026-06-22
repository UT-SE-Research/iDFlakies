package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import org.junit.Test;
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
        // Ordering 0: [P, V, N]  V FAILS
        TestOrderRecord ord0 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");
        // Ordering 1: [P, V, N]  V FAILS
        TestOrderRecord ord1 = order(Arrays.asList(P, V, N), P, "PASS", V, "FAILURE", N, "PASS");
        // Ordering 2: [N, V, P]  V PASSES
        TestOrderRecord ord2 = order(Arrays.asList(N, V, P), N, "PASS", V, "PASS", P, "PASS");

        RankFOScorer scorer = new RankFOScorer(
            RankingHeuristic.of(HeuristicType.METHODS_BEFORE_VICTIM));
        List<ScoredCandidate> ranked = scorer.score(V, Arrays.asList(ord0, ord1, ord2),
            OdType.VICTIM_POLLUTER);

        assertEquals(2, ranked.size());
        // P ranked first with polluterScore=1.0, nonPolluterScore=0.0
        assertEquals(P, ranked.get(0).getTestName());
        assertEquals(1.0, ranked.get(0).getPolluterScore(), DELTA);
        assertEquals(0.0, ranked.get(0).getNonPolluterScore(), DELTA);
        // N ranked second with polluterScore=0.0, nonPolluterScore=1.0
        assertEquals(N, ranked.get(1).getTestName());
        assertEquals(0.0, ranked.get(1).getPolluterScore(), DELTA);
        assertEquals(1.0, ranked.get(1).getNonPolluterScore(), DELTA);
    }

    @Test
    public void empty_orderings_returns_empty_list() {
        RankFOScorer scorer = new RankFOScorer(RankingHeuristic.of(HeuristicType.SIMPLE_VOTE));
        assertTrue(scorer.score(V, Arrays.asList(), OdType.VICTIM_POLLUTER).isEmpty());
    }

    @Test
    public void victim_not_in_any_ordering_returns_empty_list() {
        TestOrderRecord ord = order(Arrays.asList(P, N), P, "PASS", N, "PASS");
        RankFOScorer scorer = new RankFOScorer(RankingHeuristic.of(HeuristicType.SIMPLE_VOTE));
        assertTrue(scorer.score(V, Arrays.asList(ord), OdType.VICTIM_POLLUTER).isEmpty());
    }

    @Test
    public void single_ordering_produces_zero_class_scores() {
        // i=0 only: no consecutive pair → no class score accumulation
        TestOrderRecord ord = order(Arrays.asList(P, V), P, "PASS", V, "FAILURE");
        RankFOScorer scorer = new RankFOScorer(RankingHeuristic.of(HeuristicType.SIMPLE_VOTE));
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord), OdType.VICTIM_POLLUTER);
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getPolluterScore(), DELTA);
        assertEquals(0.0, result.get(0).getNonPolluterScore(), DELTA);
    }

    @Test
    public void bss_type_orderIsRelevant_when_brittle_passes() {
        // BSS: brittle PASSES → orderIsRelevant=true → rank increases
        // Ordering 0: [P, V]  V PASSES  (relevant for BSS)
        TestOrderRecord ord0 = order(Arrays.asList(P, V), P, "PASS", V, "PASS");
        // Ordering 1: [P, V]  V PASSES  (relevant for BSS)
        TestOrderRecord ord1 = order(Arrays.asList(P, V), P, "PASS", V, "PASS");
        // After ord0: rank[P]=1.0 (i=0, no accumulation)
        // After ord1: rank[P]=2.0; diff=1.0 ↑ → polluterScore[P]=1.0

        RankFOScorer scorer = new RankFOScorer(RankingHeuristic.of(HeuristicType.SIMPLE_VOTE));
        List<ScoredCandidate> result = scorer.score(V, Arrays.asList(ord0, ord1),
            OdType.BRITTLE_STATESETTER);

        assertEquals(1, result.size());
        assertEquals(P, result.get(0).getTestName());
        assertEquals(1.0, result.get(0).getPolluterScore(), DELTA);
    }

    @Test
    public void maxOrders_cap_limits_orderings_processed() {
        // Provide 5 orderings but cap at 2 → only first 2 processed
        // 5 identical failing orderings: with cap=2, polluterScore=1.0 (one pair 0→1)
        // with cap=5, polluterScore=4.0 (four pairs 0→1, 1→2, 2→3, 3→4)
        List<TestOrderRecord> orderings = Arrays.asList(
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE"),
            order(Arrays.asList(P, V), P, "PASS", V, "FAILURE")
        );
        RankFOScorer scorer = new RankFOScorer(RankingHeuristic.of(HeuristicType.SIMPLE_VOTE), 2);
        List<ScoredCandidate> result = scorer.score(V, orderings, OdType.VICTIM_POLLUTER);
        assertEquals(1.0, result.get(0).getPolluterScore(), DELTA);
    }
}
