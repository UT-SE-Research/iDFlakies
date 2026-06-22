package edu.illinois.cs.dt.tools.minimizer.ranking;

public class DistanceToVictimHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        double delta = 1.0 / Math.max(1, distanceToTarget);
        return orderIsRelevant ? delta : -delta;
    }
}
