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
     * Returns a reordering of {@code prefix} where candidates with a higher RankFO
     * polluter score come first. Candidates not seen in any detection run keep their
     * original relative order and appear at the end.
     *
     * <p>Never throws: any failure (no detection results, bad heuristic name, I/O error)
     * causes a FINE-level log and returns {@code prefix} unchanged.
     *
     * @param prefix          candidate tests strictly before the target in the failing order
     * @param targetTest      fully-qualified name of the victim/brittle test
     * @param isolationResult result of running targetTest alone (PASS → victim; else → brittle)
     * @return reordered list, or {@code prefix} unchanged if reordering is not possible
     */
    public static List<String> reorder(
            List<String> prefix,
            String targetTest,
            Result isolationResult) {

        try {
            Path dtDir = PathManager.cachePath();
            List<TestOrderRecord> orderings =
                DetectionResultsLoader.load(dtDir, targetTest, MAX_ORDERS);

            if (orderings.isEmpty()) {
                Logger.getGlobal().log(Level.FINE,
                    "[RankFO] No detection results found for " + targetTest
                        + "; using original candidate order.");
                return prefix;
            }

            OdType odType = (isolationResult == Result.PASS)
                ? OdType.VICTIM_POLLUTER
                : OdType.BRITTLE_STATESETTER;

            String heuristicName = Configuration.config()
                .getProperty("dt.rankfo.heuristic", "DISTANCE_TO_VICTIM");
            HeuristicType hType = HeuristicType.valueOf(heuristicName);

            List<ScoredCandidate> ranked =
                new RankFOScorer(RankingHeuristic.of(hType)).score(targetTest, orderings, odType);

            // Build score lookup: test name → polluterScore
            Map<String, Double> scoreMap = new LinkedHashMap<>();
            for (ScoredCandidate sc : ranked) {
                scoreMap.put(sc.getTestName(), sc.getPolluterScore());
            }

            // Stable sort: higher polluterScore first; unscored candidates go to the end
            List<String> reordered = new ArrayList<>(prefix);
            reordered.sort(Comparator.comparingDouble(
                (String t) -> scoreMap.getOrDefault(t, Double.NEGATIVE_INFINITY)
            ).reversed());

            Logger.getGlobal().log(Level.INFO,
                "[RankFO] Reordered " + reordered.size() + " candidates for " + targetTest
                    + " using " + heuristicName + " heuristic."
                    + " Top candidate: " + (reordered.isEmpty() ? "none" : reordered.get(0)));

            return reordered;

        } catch (IllegalArgumentException e) {
            Logger.getGlobal().log(Level.FINE,
                "[RankFO] Unknown heuristic '" + Configuration.config()
                    .getProperty("dt.rankfo.heuristic", "DISTANCE_TO_VICTIM")
                    + "'; using original candidate order.");
            return prefix;
        } catch (Exception e) {
            Logger.getGlobal().log(Level.FINE,
                "[RankFO] Reordering failed (" + e.getMessage() + "); using original candidate order.");
            return prefix;
        }
    }
}
