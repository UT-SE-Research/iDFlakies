package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RankFOCandidateReorderer {

    static final int MAX_ORDERS = 20;

    // ── Public entry points ─────────────────────────────────────────────────

    /**
     * Phase 4 entry point: heuristic is supplied explicitly by {@link
     * edu.illinois.cs.dt.tools.minimizer.MinimizerStrategy#heuristic()}.
     * No system-property reads.
     */
    public static List<String> reorder(
            List<String> prefix,
            String targetTest,
            Result isolationResult,
            HeuristicType heuristic) {
        return core(prefix, targetTest, isolationResult, PathManager.cachePath(), heuristic);
    }

    // ── Package-private (tests supply an explicit dtDir) ────────────────────

    /**
     * Package-private: reads {@code dt.rankfo.heuristic} property (default DISTANCE).
     * Used by existing cache/reorder unit tests that inject a temp directory.
     */
    static List<String> reorder(
            List<String> prefix,
            String targetTest,
            Result isolationResult,
            Path dtDir) {
        String heuristicName = Configuration.config()
                .getProperty("dt.rankfo.heuristic", "DISTANCE");
        try {
            return core(prefix, targetTest, isolationResult, dtDir,
                    HeuristicType.valueOf(heuristicName));
        } catch (IllegalArgumentException e) {
            Logger.getGlobal().log(Level.FINE,
                    "[RankFO] Unknown heuristic '" + heuristicName
                            + "'; using original candidate order.");
            return prefix;
        }
    }

    // ── Core implementation ─────────────────────────────────────────────────

    private static List<String> core(
            List<String> prefix,
            String targetTest,
            Result isolationResult,
            Path dtDir,
            HeuristicType hType) {
        try {
            OdType odType = (isolationResult == Result.PASS)
                    ? OdType.VICTIM_POLLUTER
                    : OdType.BRITTLE_STATESETTER;

            List<ScoredCandidate> ranked =
                    RankFOScoreCache.load(dtDir, targetTest, hType, odType);

            if (ranked == null) {
                long startMs = System.currentTimeMillis();

                List<TestOrderRecord> orderings =
                        DetectionResultsLoader.load(dtDir, targetTest, MAX_ORDERS);

                if (orderings.isEmpty()) {
                    Logger.getGlobal().log(Level.FINE,
                            "[RankFO] No detection results found for " + targetTest
                                    + "; using original candidate order.");
                    return prefix;
                }

                ranked = new RankFOScorer(hType).score(targetTest, orderings, odType);

                long elapsedMs = System.currentTimeMillis() - startMs;
                Logger.getGlobal().log(Level.INFO,
                        "[RankFO] Scored " + ranked.size() + " candidates for "
                                + targetTest + " in " + elapsedMs + "ms"
                                + " (heuristic=" + hType + ")");

                RankFOScoreCache.save(dtDir, targetTest, hType, odType, ranked);
            } else {
                Logger.getGlobal().log(Level.INFO,
                        "[RankFO] Loaded scores from cache for " + targetTest
                                + " (heuristic=" + hType + ")");
            }

            Map<String, Double> scoreMap = new LinkedHashMap<>();
            for (ScoredCandidate sc : ranked) {
                scoreMap.put(sc.getTestName(), sc.getPolluterScore());
            }

            List<String> reordered = new ArrayList<>(prefix);
            reordered.sort(Comparator.comparingDouble(
                    (String t) -> scoreMap.getOrDefault(t, Double.NEGATIVE_INFINITY)
            ).reversed());

            Logger.getGlobal().log(Level.INFO,
                    "[RankFO] Reordered " + reordered.size() + " candidates for "
                            + targetTest + " using " + hType + " heuristic."
                            + " Top candidate: "
                            + (reordered.isEmpty() ? "none" : reordered.get(0)));

            return reordered;

        } catch (Exception e) {
            Logger.getGlobal().log(Level.FINE,
                    "[RankFO] Reordering failed (" + e.getMessage()
                            + "); using original candidate order.");
            return prefix;
        }
    }
}
