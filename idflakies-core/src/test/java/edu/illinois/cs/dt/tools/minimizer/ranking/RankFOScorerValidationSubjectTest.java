package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;

/**
 * Hardcoded-expected-value regression test for RankFOScorer, driven by the real
 * validation-subject fixture (4 candidates, 10 sampled orderings; see
 * scripts/rankfo-validation/generate_fixtures.py, seed=42).
 *
 * Per review feedback: manually matching computed values to tool output is not a permanent
 * regression check. This test hardcodes the literal expected polluterScore/nonPolluterScore
 * for every candidate, for all 5 heuristics -- computed by
 * scripts/rankfo-validation/verify_scores.py's expected_scores(), which is itself
 * unit-tested against hand-computed toy inputs in
 * scripts/rankfo-validation/test_verify_scores.py. If a future change to RankFOScorer's
 * math regresses, this test fails here in idflakies-core's own suite, not only in the
 * separate validation-subject CI run.
 */
public class RankFOScorerValidationSubjectTest {
    private static final double DELTA = 1e-9;

    private static final String V = "com.example.VictimTest.victim";
    private static final String POLLUTER = "com.example.PolluterTest.pollute";
    private static final String CLEANER = "com.example.CleanerTest.clean";
    private static final String INNOCENT = "com.example.InnocentTest.innocent";
    private static final String INNOCENT2 = "com.example.Innocent2Test.innocent2";

    private TestOrderRecord order(Result victimResult, String... nonVictimOrder) {
        List<String> tests = new ArrayList<>(Arrays.asList(nonVictimOrder));
        tests.add(V);
        Map<String, Result> results = new HashMap<>();
        for (String t : nonVictimOrder) {
            results.put(t, Result.PASS);
        }
        results.put(V, victimResult);
        return new TestOrderRecord(tests, results);
    }

    // The 10 orderings sampled by generate_fixtures.py (seed=42) from validation-subject's
    // real .dtfixingtools fixture, in original run0..run9 order. Victim result mirrors real
    // pollution semantics: PolluterTest sets SharedState.polluted=true, CleanerTest resets
    // it false, so the victim ERRORs iff a Polluter run precedes it with no later Cleaner.
    private List<TestOrderRecord> realOrderings() {
        return Arrays.asList(
            order(Result.ERROR, CLEANER, INNOCENT, POLLUTER, INNOCENT2),   // run0
            order(Result.PASS,  POLLUTER, INNOCENT2, CLEANER, INNOCENT),   // run1
            order(Result.PASS,  POLLUTER, INNOCENT, INNOCENT2, CLEANER),   // run2
            order(Result.PASS,  INNOCENT, INNOCENT2, POLLUTER, CLEANER),   // run3
            order(Result.PASS,  INNOCENT, POLLUTER, CLEANER, INNOCENT2),   // run4
            order(Result.ERROR, CLEANER, POLLUTER, INNOCENT2, INNOCENT),   // run5
            order(Result.PASS,  POLLUTER, CLEANER, INNOCENT, INNOCENT2),   // run6
            order(Result.ERROR, CLEANER, INNOCENT2, POLLUTER, INNOCENT),   // run7
            order(Result.PASS,  POLLUTER, INNOCENT2, INNOCENT, CLEANER),   // run8
            order(Result.ERROR, INNOCENT, INNOCENT2, CLEANER, POLLUTER)    // run9
        );
    }

    private void assertScore(List<ScoredCandidate> ranked, String name,
            double expectedPolluter, double expectedNonPolluter) {
        for (ScoredCandidate sc : ranked) {
            if (sc.getTestName().equals(name)) {
                assertEquals(name + " polluterScore",
                    expectedPolluter, sc.getPolluterScore(), DELTA);
                assertEquals(name + " nonPolluterScore",
                    expectedNonPolluter, sc.getNonPolluterScore(), DELTA);
                return;
            }
        }
        throw new AssertionError(name + " not found in ranked candidates: " + ranked);
    }

    @Test
    public void plusOne_hardcoded_expected_scores() {
        List<ScoredCandidate> ranked = new RankFOScorer(HeuristicType.PLUS_ONE)
            .score(V, realOrderings(), OdType.VICTIM_POLLUTER);
        // All 4 candidates always co-occur (count=4 every order), so PLUS_ONE's delta
        // depends only on relevant/count -- identical for every candidate every order.
        assertEquals(4, ranked.size());
        assertScore(ranked, POLLUTER, 4.0, 6.0);
        assertScore(ranked, CLEANER, 4.0, 6.0);
        assertScore(ranked, INNOCENT, 4.0, 6.0);
        assertScore(ranked, INNOCENT2, 4.0, 6.0);
    }

    @Test
    public void methods_hardcoded_expected_scores() {
        List<ScoredCandidate> ranked = new RankFOScorer(HeuristicType.METHODS)
            .score(V, realOrderings(), OdType.VICTIM_POLLUTER);
        assertScore(ranked, POLLUTER, 1.0, 1.5);
        assertScore(ranked, CLEANER, 1.0, 1.5);
        assertScore(ranked, INNOCENT, 1.0, 1.5);
        assertScore(ranked, INNOCENT2, 1.0, 1.5);
    }

    @Test
    public void distance_hardcoded_expected_scores() {
        List<ScoredCandidate> ranked = new RankFOScorer(HeuristicType.DISTANCE)
            .score(V, realOrderings(), OdType.VICTIM_POLLUTER);
        // Distance depends on each candidate's own index per order, so scores differ --
        // unlike PLUS_ONE/METHODS above, this heuristic actually discriminates candidates
        // on this fixture (its full-precision values were cross-checked against an
        // independent from-scratch Python re-implementation, see docs/experiments/).
        assertScore(ranked, CLEANER,   1.25,               4.333333333333334);
        assertScore(ranked, INNOCENT2, 2.1666666666666665, 3.5);
        assertScore(ranked, INNOCENT,  2.583333333333333,  2.833333333333333);
        assertScore(ranked, POLLUTER,  2.333333333333333,  1.8333333333333333);
    }

    @Test
    public void combinedPlusOneDistance_hardcoded_expected_scores() {
        List<ScoredCandidate> ranked = new RankFOScorer(HeuristicType.COMBINED_PLUS_ONE_DISTANCE)
            .score(V, realOrderings(), OdType.VICTIM_POLLUTER);
        assertScore(ranked, POLLUTER, 4.0, 6.0);
        assertScore(ranked, CLEANER, 4.0, 6.0);
        assertScore(ranked, INNOCENT, 4.0, 6.0);
        assertScore(ranked, INNOCENT2, 4.0, 6.0);
    }

    @Test
    public void combinedMethodsDistance_hardcoded_expected_scores() {
        List<ScoredCandidate> ranked = new RankFOScorer(HeuristicType.COMBINED_METHODS_DISTANCE)
            .score(V, realOrderings(), OdType.VICTIM_POLLUTER);
        assertScore(ranked, POLLUTER, 1.0, 1.5);
        assertScore(ranked, CLEANER, 1.0, 1.5);
        assertScore(ranked, INNOCENT, 1.0, 1.5);
        assertScore(ranked, INNOCENT2, 1.0, 1.5);
    }
}
