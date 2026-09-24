package edu.illinois.cs.dt.tools.minimizer.ranking;

/**
 * "D" (Distance) heuristic: weights a candidate by how close it ran to the victim in a
 * relevant ordering (1 / distance), on the intuition that a polluter's effect is more likely
 * to be attributed to it, and less likely to be masked by an intervening test, the closer it
 * ran to the victim.
 */
public class DistanceHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        double delta = 1.0 / Math.max(1, distanceToTarget);
        return orderIsRelevant ? delta : -delta;
    }
}
