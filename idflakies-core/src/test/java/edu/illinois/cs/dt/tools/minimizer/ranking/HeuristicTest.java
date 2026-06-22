package edu.illinois.cs.dt.tools.minimizer.ranking;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HeuristicTest {
    private static final double DELTA = 1e-9;

    @Test
    public void simpleVote_relevant_returns_positive_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.SIMPLE_VOTE);
        assertEquals(1.0, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void simpleVote_notRelevant_returns_negative_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.SIMPLE_VOTE);
        assertEquals(-1.0, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void simpleVote_ignores_count_and_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.SIMPLE_VOTE);
        assertEquals(1.0, h.scoreDelta(true, 0, 0), DELTA);
        assertEquals(1.0, h.scoreDelta(true, 100, 99), DELTA);
    }

    @Test
    public void methodsBefore_relevant_returns_one_over_count() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS_BEFORE_VICTIM);
        assertEquals(0.25, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void methodsBefore_notRelevant_returns_negative_one_over_count() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS_BEFORE_VICTIM);
        assertEquals(-0.25, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void methodsBefore_zeroCount_returns_zero() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS_BEFORE_VICTIM);
        assertEquals(0.0, h.scoreDelta(true, 0, 0), DELTA);
    }

    @Test
    public void methodsBefore_countOne_returns_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.METHODS_BEFORE_VICTIM);
        assertEquals(1.0, h.scoreDelta(true, 1, 0), DELTA);
    }

    @Test
    public void distanceToVictim_relevant_returns_one_over_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE_TO_VICTIM);
        assertEquals(0.5, h.scoreDelta(true, 4, 2), DELTA);
    }

    @Test
    public void distanceToVictim_notRelevant_returns_negative_one_over_dist() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE_TO_VICTIM);
        assertEquals(-0.5, h.scoreDelta(false, 4, 2), DELTA);
    }

    @Test
    public void distanceToVictim_zeroDistance_clamps_to_one() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE_TO_VICTIM);
        assertEquals(1.0, h.scoreDelta(true, 4, 0), DELTA);
    }

    @Test
    public void distanceToVictim_distThree_returns_one_third() {
        RankingHeuristic h = RankingHeuristic.of(HeuristicType.DISTANCE_TO_VICTIM);
        assertEquals(1.0 / 3.0, h.scoreDelta(true, 4, 3), DELTA);
    }
}
