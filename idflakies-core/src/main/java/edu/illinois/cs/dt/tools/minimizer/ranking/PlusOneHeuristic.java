package edu.illinois.cs.dt.tools.minimizer.ranking;

/**
 * "P1" (Plus-One) heuristic: the simplest scheme -- every candidate present in a relevant
 * ordering gets a flat +1 (or -1 if the ordering was irrelevant), regardless of position or
 * how many other candidates were also present. Intuition: just count how often a candidate
 * shows up alongside the victim's pollution, with no positional weighting at all.
 */
public class PlusOneHeuristic implements RankingHeuristic {
    @Override
    public double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget) {
        return orderIsRelevant ? 1.0 : -1.0;
    }
}
