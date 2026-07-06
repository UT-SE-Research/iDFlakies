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

    /**
     * Public entry point. Delegates to the package-private overload using
     * the project's real {@code .dtfixingtools/} directory.
     */
    public static List<String> reorder(
            List<String> prefix,
            String targetTest,
            Result isolationResult) {
        return reorder(prefix, targetTest, isolationResult, PathManager.cachePath());
    }

    /**
     * Package-private overload that accepts an explicit {@code dtDir} so tests
     * can supply a temp directory without touching {@link PathManager}.
     *
     * <p>Scores are cached in {@code <dtDir>/rankfo-scores/} and reused on
     * subsequent calls as long as the detection results directory has not been
     * modified (i.e., no new {@code detect} run has added data). If the cache
     * is stale or absent, scores are recomputed and persisted.
     *
     * <p>Never throws: any failure causes a FINE-level log and returns
     * {@code prefix} unchanged.
     */
    static List<String> reorder(
            List<String> prefix,
            String targetTest,
            Result isolationResult,
            Path dtDir) {

        try {
            OdType odType = (isolationResult == Result.PASS)
                    ? OdType.VICTIM_POLLUTER
                    : OdType.BRITTLE_STATESETTER;

            String heuristicName = Configuration.config()
                    .getProperty("dt.rankfo.heuristic", "DISTANCE");
            HeuristicType hType = HeuristicType.valueOf(heuristicName);

            // ── cache lookup ──────────────────────────────────────────────
            List<ScoredCandidate> ranked =
                    RankFOScoreCache.load(dtDir, targetTest, hType, odType);

            if (ranked == null) {
                // Cache miss: load detection results and score candidates
                long startMs = System.currentTimeMillis();

                List<TestOrderRecord> orderings =
                        DetectionResultsLoader.load(dtDir, targetTest, MAX_ORDERS);

                if (orderings.isEmpty()) {
                    Logger.getGlobal().log(Level.FINE,
                            "[RankFO] No detection results found for " + targetTest
                                    + "; using original candidate order.");
                    return prefix;
                }

                ranked = new RankFOScorer(hType)
                        .score(targetTest, orderings, odType);

                long elapsedMs = System.currentTimeMillis() - startMs;
                Logger.getGlobal().log(Level.INFO,
                        "[RankFO] Scored " + ranked.size() + " candidates for "
                                + targetTest + " in " + elapsedMs + "ms"
                                + " (heuristic=" + heuristicName + ")");

                RankFOScoreCache.save(dtDir, targetTest, hType, odType, ranked);
            } else {
                Logger.getGlobal().log(Level.INFO,
                        "[RankFO] Loaded scores from cache for " + targetTest
                                + " (heuristic=" + heuristicName + ")");
            }

            // ── sort prefix by polluter score DESC ────────────────────────
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
                            + targetTest + " using " + heuristicName + " heuristic."
                            + " Top candidate: "
                            + (reordered.isEmpty() ? "none" : reordered.get(0)));

            return reordered;

        } catch (IllegalArgumentException e) {
            Logger.getGlobal().log(Level.FINE,
                    "[RankFO] Unknown heuristic '"
                            + Configuration.config()
                                    .getProperty("dt.rankfo.heuristic", "DISTANCE")
                            + "'; using original candidate order.");
            return prefix;
        } catch (Exception e) {
            Logger.getGlobal().log(Level.FINE,
                    "[RankFO] Reordering failed (" + e.getMessage()
                            + "); using original candidate order.");
            return prefix;
        }
    }
}
