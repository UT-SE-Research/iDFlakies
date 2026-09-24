#!/usr/bin/env python3
"""Standalone tests for verify_scores.py's own scoring formulas, independent of both the
real Java implementation and the real validation-subject fixture. Uses small, hand-computed
toy orderings so the expected numbers here are derived by hand from the RankFO paper's
formulas directly, not copied from any tool's output. This is what establishes that
verify_scores.py itself is correct -- the real fixture comparisons only establish that Java
agrees with this script, which is a different (and weaker) claim.

Toy fixture: 2 candidates (A, B), 3 orderings, victim V.
  order0: subOrder=[A, B], victim FAILS (relevant)   -> count=2, A idx=0 dist=2, B idx=1 dist=1
  order1: subOrder=[B],    victim PASSES (irrelevant) -> count=1, B idx=0 dist=1
  order2: subOrder=[A],    victim FAILS (relevant)   -> count=1, A idx=0 dist=1

Hand-computed running totals (worked on paper, see comments per heuristic below).
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_scores import score_delta, expected_scores  # noqa: E402

A = "com.example.ATest.a"
B = "com.example.BTest.b"
V = "com.example.VictimTest.victim"

TOY_ORDERINGS = [
    {
        "testOrder": [A, B, V],
        "results": {V: {"result": "ERROR"}},
    },
    {
        "testOrder": [B, V],
        "results": {V: {"result": "PASS"}},
    },
    {
        "testOrder": [A, V],
        "results": {V: {"result": "ERROR"}},
    },
]


class ScoreDeltaUnitTests(unittest.TestCase):
    """Isolated tests of the single-candidate, single-order delta formula, one per heuristic
    branch, with values computed directly from the paper's definitions -- not from any run."""

    def test_plus_one_relevant_is_positive_one_regardless_of_position(self):
        self.assertEqual(score_delta("PLUS_ONE", relevant=True, count=5, dist=3), 1.0)

    def test_plus_one_irrelevant_is_negative_one_regardless_of_position(self):
        self.assertEqual(score_delta("PLUS_ONE", relevant=False, count=5, dist=3), -1.0)

    def test_methods_relevant_is_one_over_prefix_size(self):
        # 1/count = 1/4
        self.assertAlmostEqual(score_delta("METHODS", relevant=True, count=4, dist=1), 0.25)

    def test_methods_irrelevant_is_negative_one_over_prefix_size(self):
        self.assertAlmostEqual(score_delta("METHODS", relevant=False, count=4, dist=1), -0.25)

    def test_methods_zero_count_is_zero_not_division_error(self):
        self.assertEqual(score_delta("METHODS", relevant=True, count=0, dist=0), 0.0)

    def test_distance_relevant_is_one_over_distance(self):
        # 1/dist = 1/2
        self.assertAlmostEqual(score_delta("DISTANCE", relevant=True, count=5, dist=2), 0.5)

    def test_distance_irrelevant_is_negative_one_over_distance(self):
        self.assertAlmostEqual(score_delta("DISTANCE", relevant=False, count=5, dist=2), -0.5)

    def test_distance_floors_at_one_to_avoid_division_by_zero(self):
        # max(1, dist) -- dist=0 must not divide by zero
        self.assertEqual(score_delta("DISTANCE", relevant=True, count=1, dist=0), 1.0)

    def test_combined_heuristics_reuse_plus_one_and_methods_deltas(self):
        self.assertEqual(
            score_delta("COMBINED_PLUS_ONE_DISTANCE", relevant=True, count=5, dist=3), 1.0)
        self.assertAlmostEqual(
            score_delta("COMBINED_METHODS_DISTANCE", relevant=True, count=4, dist=1), 0.25)


class ExpectedScoresToyFixtureTests(unittest.TestCase):
    """Tests expected_scores() end-to-end against the 3-order toy fixture above, with every
    expected number hand-derived per heuristic (see module docstring for the raw deltas)."""

    def test_plus_one(self):
        # order0 (relevant): A +1, B +1            -> current A=1  B=1   (both diff from 0: polluter+=1 each)
        # order1 (irrelevant): B -1                -> current A=1  B=0   (B diff -1: nonPolluter[B]+=1)
        # order2 (relevant): A +1                  -> current A=2  B=0   (A diff +1: polluter[A]+=1)
        polluter, non_polluter = expected_scores("PLUS_ONE", TOY_ORDERINGS)
        self.assertEqual(polluter[A], 2.0)
        self.assertEqual(polluter[B], 1.0)
        self.assertEqual(non_polluter[A], 0.0)
        self.assertEqual(non_polluter[B], 1.0)

    def test_methods(self):
        # order0: count=2, delta=+-0.5 each         -> current A=0.5  B=0.5   (polluter += 0.5 each)
        # order1: count=1, B delta=-1.0             -> current A=0.5  B=-0.5  (nonPolluter[B] += 1.0)
        # order2: count=1, A delta=+1.0             -> current A=1.5  B=-0.5  (polluter[A] += 1.0)
        polluter, non_polluter = expected_scores("METHODS", TOY_ORDERINGS)
        self.assertAlmostEqual(polluter[A], 1.5)
        self.assertAlmostEqual(polluter[B], 0.5)
        self.assertAlmostEqual(non_polluter[A], 0.0)
        self.assertAlmostEqual(non_polluter[B], 1.0)

    def test_distance(self):
        # order0: A idx0 dist2 delta+0.5, B idx1 dist1 delta+1.0  -> current A=0.5 B=1.0 (polluter += those)
        # order1: B idx0 dist1 delta-1.0                          -> current A=0.5 B=0.0 (nonPolluter[B] += 1.0)
        # order2: A idx0 dist1 delta+1.0                          -> current A=1.5 B=0.0 (polluter[A] += 1.0)
        polluter, non_polluter = expected_scores("DISTANCE", TOY_ORDERINGS)
        self.assertAlmostEqual(polluter[A], 1.5)
        self.assertAlmostEqual(polluter[B], 1.0)
        self.assertAlmostEqual(non_polluter[A], 0.0)
        self.assertAlmostEqual(non_polluter[B], 1.0)

    def test_order_zero_contributes_not_silently_skipped(self):
        # Regression guard for the same class of bug fixed in RankFOScorer.java: order0 must
        # be diffed against the zero baseline, not skipped. With only order0 present and
        # relevant, A's polluterScore must be > 0, not 0.
        single_order = [TOY_ORDERINGS[0]]
        polluter, _ = expected_scores("PLUS_ONE", single_order)
        self.assertEqual(polluter[A], 1.0)
        self.assertEqual(polluter[B], 1.0)


if __name__ == "__main__":
    unittest.main()
