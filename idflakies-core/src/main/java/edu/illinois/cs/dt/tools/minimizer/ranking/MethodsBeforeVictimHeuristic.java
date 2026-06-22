package edu.illinois.cs.dt.tools.minimizer.ranking;

public class MethodsBeforeVictimHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        if (testsBeforeTarget == 0) return 0.0;
        double delta = 1.0 / testsBeforeTarget;
        return orderIsRelevant ? delta : -delta;
    }
}
