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
    private final RankingHeuristic heuristic;
    private final int maxOrders;

    public RankFOScorer(RankingHeuristic heuristic) {
        this(heuristic, 20);
    }

    public RankFOScorer(RankingHeuristic heuristic, int maxOrders) {
        this.heuristic = heuristic;
        this.maxOrders = maxOrders;
    }

    /**
     * Ports RankF_O generateRanks() + getClassScores().
     * Returns candidates sorted by polluterScore descending; ties preserve insertion order.
     */
    public List<ScoredCandidate> score(
            String targetTest,
            List<TestOrderRecord> orderings,
            OdType odType) {

        int limit = Math.min(maxOrders, orderings.size());

        // Collect all candidates (union of testsBeforeTarget across orderings, insertion order kept)
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
            if (rec.getResult(targetTest) == null) continue; // target not in this round

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
            // Candidates not in subOrder keep their current rank (intentional)

            // Accumulate class scores starting from first consecutive pair (i > 0)
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
        // Stable sort: ties preserve insertion order (matches Python sort behavior)
        result.sort((a, b) -> Double.compare(b.getPolluterScore(), a.getPolluterScore()));
        return result;
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
