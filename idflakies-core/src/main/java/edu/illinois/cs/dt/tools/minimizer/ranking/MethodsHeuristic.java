package edu.illinois.cs.dt.tools.minimizer.ranking;

/**
 * "M" (#Methods) heuristic: weights a candidate by 1 / (number of tests before the victim in
 * that ordering), on the intuition that a candidate implicated in a *smaller* suspect set is
 * more likely to be the actual polluter than one implicated alongside many others.
 */
public class MethodsHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        if (testsBeforeTarget == 0) {
            return 0.0;
        }
        double delta = 1.0 / testsBeforeTarget;
        return orderIsRelevant ? delta : -delta;
    }
}
