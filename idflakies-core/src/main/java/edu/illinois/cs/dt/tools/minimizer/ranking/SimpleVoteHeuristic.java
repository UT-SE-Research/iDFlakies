package edu.illinois.cs.dt.tools.minimizer.ranking;

public class SimpleVoteHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        return orderIsRelevant ? 1.0 : -1.0;
    }
}
