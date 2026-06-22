package edu.illinois.cs.dt.tools.minimizer.ranking;

public interface RankingHeuristic {

    /**
     * Score delta for one candidate in one test execution ordering.
     *
     * @param orderIsRelevant  true = increase rank (VP: victim failed; BSS: brittle passed)
     * @param testsBeforeTarget count of tests strictly before the target in this ordering
     * @param distanceToTarget  count of tests strictly between candidate and target
     */
    double scoreDelta(boolean orderIsRelevant, int testsBeforeTarget, int distanceToTarget);

    static RankingHeuristic of(HeuristicType type) {
        switch (type) {
            case SIMPLE_VOTE:            return new SimpleVoteHeuristic();
            case METHODS_BEFORE_VICTIM:  return new MethodsBeforeVictimHeuristic();
            case DISTANCE_TO_VICTIM:     return new DistanceToVictimHeuristic();
            default: throw new IllegalArgumentException("Unknown heuristic: " + type);
        }
    }
}
