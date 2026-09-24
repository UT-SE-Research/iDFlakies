package edu.illinois.cs.dt.tools.minimizer.ranking;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HeuristicTest {
    private static final double DELTA = 1e-9;

    // ── Plus One (+1) ─────────────────────────────────────────────────────

    @Test
    public void plusOne_relevant_returns_positive_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.PLUS_ONE);
        assertEquals(1.0, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void plusOne_notRelevant_returns_negative_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.PLUS_ONE);
        assertEquals(-1.0, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void plusOne_ignores_count_and_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.PLUS_ONE);
        assertEquals(1.0, h.scoreDelta(true, 0, 0), DELTA);
        assertEquals(1.0, h.scoreDelta(true, 100, 99), DELTA);
    }

    // ── #Methods (#M) ─────────────────────────────────────────────────────

    @Test
    public void methods_relevant_returns_one_over_count() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS);
        assertEquals(0.25, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void methods_notRelevant_returns_negative_one_over_count() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS);
        assertEquals(-0.25, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void methods_zeroCount_returns_zero() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS);
        assertEquals(0.0, h.scoreDelta(true, 0, 0), DELTA);
    }

    @Test
    public void methods_countOne_returns_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS);
        assertEquals(1.0, h.scoreDelta(true, 1, 0), DELTA);
    }

    // ── Distance (D) ──────────────────────────────────────────────────────

    @Test
    public void distance_relevant_returns_one_over_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE);
        assertEquals(0.5, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void distance_notRelevant_returns_negative_one_over_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE);
        assertEquals(-0.5, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void distance_zeroDistance_clamps_to_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE);
        assertEquals(1.0, h.scoreDelta(true, 4, 0), DELTA);
    }

    @Test
    public void distance_distThree_returns_one_third() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE);
        assertEquals(1.0 / 3.0, h.scoreDelta(true, 4, 3), DELTA);
    }

    // ── Combined: factory returns correct base heuristic ─────────────────

    @Test
    public void combined_plus_one_distance_factory_returns_plus_one_behavior() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.COMBINED_PLUS_ONE_DISTANCE);
        assertEquals(1.0, h.scoreDelta(true, 4, 2), DELTA);
        assertEquals(-1.0, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void combined_methods_distance_factory_returns_methods_behavior() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.COMBINED_METHODS_DISTANCE);
        assertEquals(0.25, h.scoreDelta(true, 4, 2), DELTA);
        assertEquals(-0.25, h.scoreDelta(false, 4, 2), DELTA);
    }
}
