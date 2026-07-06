package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RankFOScorer {
    private final HeuristicType heuristicType;
    private final RankingHeuristic heuristic;
    private final int maxOrders;

    public RankFOScorer(HeuristicType heuristicType) {
        this(heuristicType, 20);
    }

    public RankFOScorer(HeuristicType heuristicType, int maxOrders) {
        this.heuristicType = heuristicType;
        this.heuristic = RankingHeuristic.of(heuristicType);
        this.maxOrders = maxOrders;
    }

    /**
     * Ports RankF_O generateRanks() + getClassScores().
     * Returns candidates sorted by polluterScore descending.
     * For Combined (+1,D) and Combined (#M,D): ties in polluterScore are broken by
     * distance to victim in the last valid ordering (closer = higher rank), matching
     * the paper's Figure 4 description.
     */
    public List<ScoredCandidate> score(
            String targetTest,
            List<TestOrderRecord> orderings,
            OdType odType) {

        int limit = Math.min(maxOrders, orderings.size());

        Set<String> candidates = new LinkedHashSet<>();
        for (int i = 0; i < limit; i++) {
            candidates.addAll(orderings.get(i).testsBeforeTarget(targetTest));
        }
        if (candidates.isEmpty()) return Collections.emptyList();

        Map<String, Double> currentRank    = new HashMap<>();
        Map<String, Double> polluterScore  = new HashMap<>();
        Map<String, Double> nonPolluterScore = new HashMap<>();
        for (String c : candidates) {
            currentRank.put(c, 0.0);
            polluterScore.put(c, 0.0);
            nonPolluterScore.put(c, 0.0);
        }

        for (int i = 0; i < limit; i++) {
            TestOrderRecord rec = orderings.get(i);
            if (rec.getResult(targetTest) == null) continue;

            Map<String, Double> previousRank = new HashMap<>(currentRank);

            List<String> subOrder = rec.testsBeforeTarget(targetTest);
            boolean relevant = isRelevant(rec, targetTest, odType);
            int count = subOrder.size();
            for (int idx = 0; idx < count; idx++) {
                String c = subOrder.get(idx);
                if (!currentRank.containsKey(c)) continue;
                int dist = count - 1 - idx; // tests strictly between candidate and target
                double delta = heuristic.scoreDelta(relevant, count, dist);
                currentRank.put(c, currentRank.get(c) + delta);
            }

            if (i > 0) {
                for (String c : candidates) {
                    double curr = currentRank.get(c);
                    double prev = previousRank.getOrDefault(c, 0.0);
                    double diff = Math.abs(curr - prev);
                    if (curr > prev) {
                        polluterScore.put(c, polluterScore.get(c) + diff);
                    } else if (curr < prev) {
                        nonPolluterScore.put(c, nonPolluterScore.get(c) + diff);
                    }
                }
            }
        }

        List<ScoredCandidate> result = new ArrayList<>();
        for (String c : candidates) {
            result.add(new ScoredCandidate(c, polluterScore.get(c), nonPolluterScore.get(c)));
        }

        if (isCombined()) {
            // Paper Fig 4: ties in polluterScore broken by distance to victim in last ordering.
            // Smaller distance (count - idx) = closer to victim = higher rank.
            final Map<String, Integer> lastDist = lastOrderDistances(targetTest, orderings, limit);
            result.sort((a, b) -> {
                int cmp = Double.compare(b.getPolluterScore(), a.getPolluterScore());
                if (cmp != 0) return cmp;
                int da = lastDist.getOrDefault(a.getTestName(), Integer.MAX_VALUE);
                int db = lastDist.getOrDefault(b.getTestName(), Integer.MAX_VALUE);
                return Integer.compare(da, db); // ASC: smaller dist (closer) ranks first
            });
        } else {
            result.sort((a, b) -> Double.compare(b.getPolluterScore(), a.getPolluterScore()));
        }
        return result;
    }

    private boolean isCombined() {
        return heuristicType == HeuristicType.COMBINED_PLUS_ONE_DISTANCE
            || heuristicType == HeuristicType.COMBINED_METHODS_DISTANCE;
    }

    /** Distance from each candidate to victim in the last ordering that contains the victim. */
    private Map<String, Integer> lastOrderDistances(String targetTest,
            List<TestOrderRecord> orderings, int limit) {
        for (int i = limit - 1; i >= 0; i--) {
            TestOrderRecord rec = orderings.get(i);
            if (rec.getResult(targetTest) != null) {
                List<String> subOrder = rec.testsBeforeTarget(targetTest);
                int count = subOrder.size();
                Map<String, Integer> dist = new HashMap<>();
                for (int idx = 0; idx < count; idx++) {
                    dist.put(subOrder.get(idx), count - idx); // paper's indexOf(ot)-indexOf(gt)
                }
                return dist;
            }
        }
        return Collections.emptyMap();
    }

    private boolean isRelevant(TestOrderRecord rec, String targetTest, OdType odType) {
        Result r = rec.getResult(targetTest);
        if (r == null) return false;
        switch (odType) {
            case VICTIM_POLLUTER:     return r == Result.FAILURE;
            case BRITTLE_STATESETTER: return r == Result.PASS;
            default: return false;
        }
    }
}
